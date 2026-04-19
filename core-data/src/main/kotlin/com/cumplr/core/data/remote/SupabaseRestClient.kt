package com.cumplr.core.data.remote

import android.util.Log
import com.cumplr.core.data.remote.dto.UserDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SupabaseRest"
private val JSON_MT = "application/json; charset=utf-8".toMediaType()

// ─── internal request / response DTOs ────────────────────────────────────────

@Serializable
private data class SignInBody(val email: String, val password: String)

@Serializable
private data class AuthTokenResponse(
    @SerialName("access_token")  val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String  = "",
    @SerialName("token_type")    val tokenType: String     = "bearer",
    @SerialName("expires_in")    val expiresIn: Int        = 3600,
    val user: AuthUserPayload,
)

@Serializable
private data class AuthUserPayload(
    val id: String,
    val email: String? = null,
)

// ─── client ──────────────────────────────────────────────────────────────────

/**
 * Thin OkHttp wrapper that calls Supabase REST APIs directly,
 * bypassing supabase-kt for the auth step (Plan B).
 *
 * Endpoints used:
 *   POST /auth/v1/token?grant_type=password  → obtain JWT
 *   GET  /rest/v1/users?id=eq.<id>&select=*  → fetch user row
 */
@Singleton
class SupabaseRestClient @Inject constructor() {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Signs in via email/password and returns (accessToken, userId).
     * Throws on HTTP error or JSON parse failure.
     */
    fun signIn(email: String, password: String): Pair<String, String> {
        val bodyStr = json.encodeToString(SignInBody(email, password))
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/auth/v1/token?grant_type=password")
            .post(bodyStr.toRequestBody(JSON_MT))
            .header("apikey", SupabaseConfig.anonKey)
            .header("Content-Type", "application/json")
            .header("X-Client-Info", "cumplr-android/1.0")
            .build()

        Log.d(TAG, "▶ POST /auth/v1/token  email=$email")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}  body=${body.take(300)}")

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $body")
        }

        val parsed = json.decodeFromString<AuthTokenResponse>(body)
        Log.d(TAG, "signIn OK — userId=${parsed.user.id}")
        return parsed.accessToken to parsed.user.id
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    /**
     * Fetches the public.users row for [userId] using [accessToken] as Bearer.
     * Returns null if the row is not found (RLS blocking or table empty).
     */
    fun getUserById(accessToken: String, userId: String): UserDto? {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/users?id=eq.$userId&select=*")
            .get()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()

        Log.d(TAG, "▶ GET /rest/v1/users?id=eq.$userId")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}  body=${body.take(300)}")

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $body")
        }

        val list = json.decodeFromString<List<UserDto>>(body)
        Log.d(TAG, "getUserById returned ${list.size} row(s)")
        return list.firstOrNull()
    }
}
