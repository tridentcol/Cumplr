package com.cumplr.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.cumplr.core.data.remote.SupabaseRestClient
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val BUCKET = "task-photos"

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val restClient: SupabaseRestClient,
    private val sessionManager: SessionManager,
) : StorageRepository {

    override suspend fun uploadTaskPhoto(
        companyId: String,
        taskId: String,
        type: String,
        imageBytes: ByteArray,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getSession().first()
                ?: return@withContext Result.failure(Exception("Sin sesión activa"))

            val compressed = imageBytes.compressToMaxSide()
            val path = "companies/$companyId/tasks/$taskId/$type.jpg"

            val url = try {
                restClient.uploadFile(session.accessToken, BUCKET, path, compressed)
            } catch (e: Exception) {
                if (isAuthError(e) && session.refreshToken.isNotBlank()) {
                    val (newAccessToken, newRefreshToken) = restClient.refreshToken(session.refreshToken)
                    sessionManager.updateTokens(newAccessToken, newRefreshToken)
                    restClient.uploadFile(newAccessToken, BUCKET, path, compressed)
                } else {
                    throw e
                }
            }

            Result.success(url)
        } catch (e: Exception) {
            Result.failure(Exception("Error al subir imagen: ${e.message}"))
        }
    }

    private fun isAuthError(e: Exception): Boolean {
        val msg = e.message ?: ""
        return msg.contains("HTTP 401") || msg.contains("HTTP 403") ||
            msg.contains("Unauthorized") || msg.contains("exp")
    }

    private fun ByteArray.compressToMaxSide(maxSide: Int = 800, quality: Int = 80): ByteArray {
        val original = BitmapFactory.decodeByteArray(this, 0, size) ?: return this
        val scale = minOf(maxSide.toFloat() / original.width, maxSide.toFloat() / original.height, 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true,
            )
        } else original
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }
}
