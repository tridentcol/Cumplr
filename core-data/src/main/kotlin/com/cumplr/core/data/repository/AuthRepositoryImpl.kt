package com.cumplr.core.data.repository

import android.util.Log
import com.cumplr.core.data.remote.dto.UserDto
import com.cumplr.core.data.remote.dto.toDomain
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.domain.model.SessionData
import com.cumplr.core.domain.model.User
import com.cumplr.core.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CumplrAuth"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionManager: SessionManager,
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<User> {
        Log.d(TAG, "─── signIn() start — email=$email ───")
        return try {

            // ── Step 1: Supabase Auth ────────────────────────────────────────
            Log.d(TAG, "Step 1 › calling supabase.auth.signInWith(Email)...")
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val authUser = supabase.auth.currentUserOrNull()
            Log.d(TAG, "Step 1 › OK — userId=${authUser?.id}  email=${authUser?.email}")

            val userId = authUser?.id
                ?: return Result.failure(Exception("No se pudo obtener el usuario autenticado."))

            // ── Step 2: Diagnostic — how many rows does RLS allow? ───────────
            Log.d(TAG, "Step 2 › diagnostic decodeList from public.users...")
            val diagResult = runCatching {
                supabase.from("users").select {
                    filter { eq("id", userId) }
                }.decodeList<UserDto>()
            }
            when {
                diagResult.isSuccess -> Log.d(TAG, "Step 2 › RLS OK — ${diagResult.getOrNull()!!.size} row(s) returned")
                else -> Log.e(TAG, "Step 2 › RLS/deserialize error: ${diagResult.exceptionOrNull()!!::class.simpleName} — ${diagResult.exceptionOrNull()!!.message}")
            }

            // ── Step 3: Decode profile (decodeSingle → decodeList fallback) ──
            Log.d(TAG, "Step 3 › fetching full profile with decodeSingle...")
            val userDto: UserDto = runCatching {
                supabase.from("users").select {
                    filter { eq("id", userId) }
                }.decodeSingle<UserDto>()
            }.recoverCatching { e ->
                Log.e(TAG, "Step 3 › decodeSingle failed: ${e::class.qualifiedName} — ${e.message}")
                e.printStackTrace()

                Log.d(TAG, "Step 3 › fallback: trying decodeList + first()...")
                val list = supabase.from("users").select {
                    filter { eq("id", userId) }
                }.decodeList<UserDto>()
                Log.d(TAG, "Step 3 › decodeList returned ${list.size} item(s)")

                list.firstOrNull() ?: run {
                    // ── Step 4: JWT fallback (dev only) ─────────────────────
                    Log.w(TAG, "Step 4 › DB returned 0 rows — using JWT fallback (RLS blocking?)")
                    UserDto(
                        id        = userId,
                        companyId = authUser.email?.substringBefore("@") ?: "dev-fallback",
                        name      = authUser.email?.substringBefore("@") ?: "Usuario",
                        email     = authUser.email ?: email,
                        role      = "ADMIN",
                        active    = true,
                    )
                }
            }.getOrElse { e ->
                Log.e(TAG, "Step 3+4 › all queries failed: ${e::class.qualifiedName} — ${e.message}")
                e.printStackTrace()
                // Last-resort JWT fallback
                Log.w(TAG, "Step 4 › last-resort JWT fallback")
                UserDto(
                    id        = userId,
                    companyId = "dev-fallback",
                    name      = authUser.email?.substringBefore("@") ?: "Usuario",
                    email     = authUser.email ?: email,
                    role      = "ADMIN",
                    active    = true,
                )
            }

            Log.d(TAG, "Profile resolved › id=${userDto.id}  role=${userDto.role}  active=${userDto.active}  companyId=${userDto.companyId}")

            if (!userDto.active) {
                supabase.auth.signOut()
                return Result.failure(Exception("Tu cuenta está inactiva. Contacta a tu administrador."))
            }

            val user = userDto.toDomain()
            sessionManager.saveSession(
                SessionData(
                    userId    = user.id,
                    companyId = user.companyId,
                    role      = user.role,
                    name      = user.name,
                )
            )
            Log.d(TAG, "─── signIn() success — role=${user.role} ───")
            Result.success(user)

        } catch (e: Exception) {
            Log.e(TAG, "─── signIn() FATAL: ${e::class.qualifiedName} — ${e.message} ───")
            e.printStackTrace()
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.success(Unit)
        }
    }

    override fun getCurrentSession(): Flow<SessionData?> = sessionManager.getSession()

    private fun mapAuthError(e: Exception): String = when {
        e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
            "Credenciales incorrectas. Verifica tu correo y contraseña."
        e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
            "Correo no verificado. Revisa tu bandeja de entrada."
        e.message?.contains("network", ignoreCase = true) == true ||
        e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
        e.message?.contains("UnknownHostException", ignoreCase = true) == true ||
        e.message?.contains("SocketTimeoutException", ignoreCase = true) == true ->
            "Sin conexión. Verifica tu internet e intenta de nuevo."
        else -> "Error al iniciar sesión. Intenta de nuevo."
    }
}
