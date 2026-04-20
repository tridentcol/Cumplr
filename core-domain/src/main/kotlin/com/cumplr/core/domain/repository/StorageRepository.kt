package com.cumplr.core.domain.repository

interface StorageRepository {
    suspend fun uploadTaskPhoto(
        companyId: String,
        taskId: String,
        type: String,
        imageBytes: ByteArray,
    ): Result<String>
}
