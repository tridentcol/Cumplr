package com.cumplr.core.data.repository

import android.util.Log
import com.cumplr.core.data.remote.SupabaseRestClient
import com.cumplr.core.data.remote.dto.UserDto
import com.cumplr.core.data.remote.dto.toDomain
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.model.SessionData
import com.cumplr.core.domain.model.User
import com.cumplr.core.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CumplrAuth"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val restClient: SupabaseRestClient,
    private val sessionManager: SessionManager,
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<User> {
        Log.d(TAG, "─── signIn() start — email=$email ───")
        return withContext(Dispatchers.IO) {
            try {

                // ── Step 1: direct REST call to /auth/v1/token ──────────────
                Log.d(TAG, "Step 1 › POST /auth/v1/token?grant_type=password")
                val (accessToken, userId) = restClient.signIn(email, password)
                Log.d(TAG, "Step 1 › OK — userId=$userId")

                // ── Step 2: fetch public.users row ───────────────────────────
                Log.d(TAG, "Step 2 › GET /rest/v1/users?id=eq.$userId")
                val userDto: UserDto = restClient.getUserById(accessToken, userId)
                    ?: run {
                        Log.w(TAG, "Step 2 › 0 rows returned (RLS?) — JWT fallback")
                        UserDto(
                            id        = userId,
                            companyId = "dev-fallback",
                            name      = email.substringBefore("@"),
                            email     = email,
                            role      = "ADMIN",
                            active    = true,
                        )
                    }

                Log.d(TAG, "Profile › id=${userDto.id}  role=${userDto.role}  active=${userDto.active}")

                if (!userDto.active) {
                    return@withContext Result.failure(
                        Exception("Tu cuenta está inactiva. Contacta a tu administrador.")
                    )
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
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override fun getCurrentSession(): Flow<SessionData?> = sessionManager.getSession()

    private fun mapAuthError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("invalid_credentials", ignoreCase = true) ||
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                "Credenciales incorrectas. Verifica tu correo y contraseña."

            msg.contains("Email not confirmed", ignoreCase = true) ->
                "Correo no verificado. Revisa tu bandeja de entrada."

            msg.contains("Database error querying schema", ignoreCase = true) ->
                "Error de configuración en el servidor Supabase (schema). " +
                "Verifica Auth Hooks en el dashboard."

            msg.contains("network", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("UnknownHostException", ignoreCase = true) ||
            msg.contains("SocketTimeoutException", ignoreCase = true) ->
                "Sin conexión. Verifica tu internet e intenta de nuevo."

            else -> "Error al iniciar sesión: $msg"
        }
    }
}
