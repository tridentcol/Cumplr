package com.cumplr.core.data.session

import android.util.Log
import com.cumplr.core.data.remote.SupabaseRestClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TokenAuth"

/**
 * Singleton helper that runs a REST operation with the current access token
 * and transparently refreshes once on HTTP 401/403.
 *
 * One shared instance ensures that concurrent callers (TaskRepository,
 * UserRepository, …) cannot race two refreshes against the same
 * refresh_token — Supabase rotates refresh tokens server-side, so a second
 * concurrent refresh would fail with an invalid_grant.
 *
 * The reactive (catch-401-and-retry) approach is preferred over a
 * clock-based proactive check because device clocks can drift, masking
 * valid tokens or refreshing good ones in a tight loop.
 */
@Singleton
class TokenAuthGuard @Inject constructor(
    private val sessionManager: SessionManager,
    private val restClient: SupabaseRestClient,
    private val authEventBus: AuthEventBus,
) {

    private val refreshMutex = Mutex()

    /**
     * Runs [op] with the current access token. If [op] throws an HTTP 401/403
     * and a refresh token is available, refreshes the session once and
     * retries [op] with the new token. Re-throws any other failure.
     */
    suspend fun <T> withValidToken(op: (String) -> T): T {
        val initial = sessionManager.getSession().first()
            ?: throw IllegalStateException("Sin sesión activa")
        if (initial.accessToken.isBlank()) throw IllegalStateException("Sin token de acceso")
        return try {
            op(initial.accessToken)
        } catch (e: Exception) {
            if (!isAuthError(e) || initial.refreshToken.isBlank()) throw e
            op(refreshIfStillStale(initial.accessToken))
        }
    }

    /**
     * Returns a token guaranteed to be at least as fresh as the one stored
     * when the caller last read it. If another caller already refreshed
     * between then and now, we reuse their result instead of burning the
     * (now-rotated) refresh token.
     */
    private suspend fun refreshIfStillStale(staleToken: String): String = refreshMutex.withLock {
        val current = sessionManager.getSession().first()
            ?: throw IllegalStateException("Sin sesión activa")
        if (current.accessToken != staleToken) return@withLock current.accessToken
        return try {
            val (newAccess, newRefresh) = restClient.refreshToken(current.refreshToken)
            sessionManager.updateTokens(newAccess, newRefresh)
            Log.d(TAG, "Token refreshed after 401")
            newAccess
        } catch (e: Exception) {
            Log.e(TAG, "Refresh token rechazado — forzando re-login: ${e.message}")
            sessionManager.clearSession()
            authEventBus.postSessionExpired()
            throw e
        }
    }

    /**
     * Returns the current access token, refreshing first if the local JWT
     * exp claim says it has expired. Used by paths that cannot retry on 401
     * (Realtime WebSocket connect). Returns null if there is no session.
     *
     * Best-effort only: parsing failures fall back to "looks expired" which
     * triggers a refresh; if the refresh also fails we return the existing
     * token and let polling recover.
     */
    suspend fun freshTokenForRealtime(): String? {
        val session = sessionManager.getSession().first() ?: return null
        if (session.accessToken.isBlank()) return null
        if (!isJwtExpired(session.accessToken) || session.refreshToken.isBlank()) {
            return session.accessToken
        }
        return try {
            refreshIfStillStale(session.accessToken)
        } catch (e: Exception) {
            Log.w(TAG, "Realtime token refresh failed: ${e.message}")
            session.accessToken
        }
    }

    private fun isAuthError(e: Exception): Boolean {
        val msg = e.message.orEmpty()
        return msg.contains("HTTP 401") || msg.contains("HTTP 403")
    }

    private fun isJwtExpired(jwt: String): Boolean = try {
        val payloadB64 = jwt.split(".").getOrNull(1) ?: return true
        val padded = payloadB64 + "=".repeat((4 - payloadB64.length % 4) % 4)
        val payload = String(android.util.Base64.decode(padded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
        val expSecs = "\"exp\"\\s*:\\s*(\\d+)".toRegex()
            .find(payload)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return true
        // 30 s skew margin — refresh if already expired or within the next half-minute.
        (System.currentTimeMillis() / 1000L) >= (expSecs - 30L)
    } catch (_: Exception) { true }
}
