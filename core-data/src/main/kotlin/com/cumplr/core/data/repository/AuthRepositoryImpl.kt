package com.cumplr.core.data.repository

import com.cumplr.core.data.remote.dto.UserDto
import com.cumplr.core.data.remote.dto.toDomain
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.model.SessionData
import com.cumplr.core.domain.model.User
import com.cumplr.core.domain.repository.AuthRepository
import android.util.Log
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
        Log.d(TAG, "signIn() called — email=$email")
        return try {
            Log.d(TAG, "Calling supabase.auth.signInWith...")
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d(TAG, "Auth OK — fetching profile from public.users")
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("No se pudo obtener el usuario autenticado."))

            Log.d(TAG, "userId=$userId")
            val userDto = supabase.from("users").select {
                filter { eq("id", userId) }
            }.decodeSingle<UserDto>()

            Log.d(TAG, "Profile fetched — role=${userDto.role} active=${userDto.active}")
            if (!userDto.active) {
                supabase.auth.signOut()
                return Result.failure(Exception("Tu cuenta está inactiva. Contacta a tu administrador."))
            }

            val user = userDto.toDomain()
            sessionManager.saveSession(
                SessionData(
                    userId = user.id,
                    companyId = user.companyId,
                    role = user.role,
                    name = user.name,
                )
            )
            Log.d(TAG, "Session saved — navigating as ${user.role}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signIn failed: ${e::class.simpleName} — ${e.message}", e)
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            // Always clear local session even if remote call fails
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
