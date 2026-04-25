package com.cumplr.core.data.remote

import android.util.Log
import com.cumplr.core.data.remote.dto.TaskDto
import com.cumplr.core.data.remote.dto.UserDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SupabaseRest"
private val JSON_MT  = "application/json; charset=utf-8".toMediaType()
private val IMAGE_MT = "image/jpeg".toMediaType()

@Serializable
private data class SignInBody(val email: String, val password: String)

@Serializable
private data class RefreshTokenBody(@SerialName("refresh_token") val refreshToken: String)

@Serializable
private data class AuthTokenResponse(
    @SerialName("access_token")  val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("token_type")    val tokenType: String    = "bearer",
    @SerialName("expires_in")    val expiresIn: Int       = 3600,
    val user: AuthUserPayload,
)

@Serializable
private data class AuthUserPayload(
    val id: String,
    val email: String? = null,
)

@Singleton
class SupabaseRestClient @Inject constructor() {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Auth ──────────────────────────────────────────────────────────────────

    // Returns Triple(accessToken, refreshToken, userId)
    fun signIn(email: String, password: String): Triple<String, String, String> {
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

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")

        val parsed = json.decodeFromString<AuthTokenResponse>(body)
        Log.d(TAG, "signIn OK — userId=${parsed.user.id}")
        return Triple(parsed.accessToken, parsed.refreshToken, parsed.user.id)
    }

    // Returns Pair(newAccessToken, newRefreshToken)
    fun refreshToken(refreshToken: String): Pair<String, String> {
        val bodyStr = json.encodeToString(RefreshTokenBody(refreshToken))
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/auth/v1/token?grant_type=refresh_token")
            .post(bodyStr.toRequestBody(JSON_MT))
            .header("apikey", SupabaseConfig.anonKey)
            .header("Content-Type", "application/json")
            .build()

        Log.d(TAG, "▶ POST /auth/v1/token?grant_type=refresh_token")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}  body=${body.take(200)}")

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")

        val parsed = json.decodeFromString<AuthTokenResponse>(body)
        return parsed.accessToken to parsed.refreshToken
    }

    // ── Users ─────────────────────────────────────────────────────────────────

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

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")

        val list = json.decodeFromString<List<UserDto>>(body)
        return list.firstOrNull()
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────

    /**
     * Returns true if the JWT will expire within [bufferSeconds] from now.
     * Reads the exp claim from the token payload without a network call.
     */
    fun isTokenExpiredOrExpiringSoon(accessToken: String, bufferSeconds: Long = 60): Boolean {
        return try {
            val payloadB64 = accessToken.split(".").getOrNull(1) ?: return true
            val padded = payloadB64 + "=".repeat((4 - payloadB64.length % 4) % 4)
            val payloadJson = String(android.util.Base64.decode(padded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
            val exp = json.parseToJsonElement(payloadJson)
                .jsonObject["exp"]?.jsonPrimitive?.longOrNull ?: return true
            val nowSecs = System.currentTimeMillis() / 1000L
            (exp - nowSecs) < bufferSeconds
        } catch (_: Exception) { true }
    }

    fun getTasks(accessToken: String, userId: String): List<TaskDto> {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/tasks?assigned_to=eq.$userId&select=*&order=deadline.asc.nullslast")
            .get()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()

        Log.d(TAG, "▶ GET /rest/v1/tasks?assigned_to=eq.$userId")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}  tasks body=${body.take(200)}")

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")

        return json.decodeFromString<List<TaskDto>>(body)
    }

    fun getCompanyTasks(accessToken: String, companyId: String): List<TaskDto> {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/tasks?company_id=eq.$companyId&select=*&order=deadline.asc.nullslast")
            .get()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()

        Log.d(TAG, "▶ GET /rest/v1/tasks?company_id=eq.$companyId")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}  tasks=${body.take(100)}")

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")
        return json.decodeFromString<List<TaskDto>>(body)
    }

    fun getCompanyUsers(accessToken: String, companyId: String): List<UserDto> {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/users?company_id=eq.$companyId&select=*")
            .get()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()

        Log.d(TAG, "▶ GET /rest/v1/users?company_id=eq.$companyId")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}")

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")
        return json.decodeFromString<List<UserDto>>(body)
    }

    fun postTask(accessToken: String, jsonBody: String) {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/tasks")
            .post(jsonBody.toRequestBody(JSON_MT))
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal,resolution=merge-duplicates")
            .build()

        Log.d(TAG, "▶ POST /rest/v1/tasks  body=${jsonBody.take(200)}")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}")

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")
    }

    fun patchTask(accessToken: String, taskId: String, jsonBody: String) {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/tasks?id=eq.$taskId")
            .patch(jsonBody.toRequestBody(JSON_MT))
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal")
            .build()

        Log.d(TAG, "▶ PATCH /rest/v1/tasks?id=eq.$taskId  body=$jsonBody")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}")

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    fun uploadFile(accessToken: String, bucket: String, path: String, bytes: ByteArray): String {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/storage/v1/object/$bucket/$path")
            .post(bytes.toRequestBody(IMAGE_MT))
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("x-upsert", "true")
            .build()

        Log.d(TAG, "▶ POST /storage/v1/object/$bucket/$path  size=${bytes.size}")
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "◀ code=${response.code}")

        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $body")

        return "${SupabaseConfig.url}/storage/v1/object/public/$bucket/$path"
    }
}
