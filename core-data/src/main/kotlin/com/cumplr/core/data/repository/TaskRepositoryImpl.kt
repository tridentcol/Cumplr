package com.cumplr.core.data.repository

import android.util.Log
import com.cumplr.core.data.local.dao.TaskDao
import com.cumplr.core.data.local.mapper.toDomain
import com.cumplr.core.data.local.mapper.toEntity
import com.cumplr.core.data.remote.SupabaseRestClient
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TaskRepo"

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val restClient: SupabaseRestClient,
    private val sessionManager: SessionManager,
) : TaskRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    override fun getMyTasks(userId: String): Flow<List<Task>> =
        taskDao.getTasksByAssignedTo(userId).map { list -> list.map { it.toDomain() } }

    override fun getTask(taskId: String): Flow<Task?> =
        taskDao.getTaskById(taskId).map { it?.toDomain() }

    override suspend fun refresh(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getSession().first()
                ?: return@withContext Result.failure(Exception("Sin sesión activa"))
            val dtos = restClient.getTasks(session.accessToken, userId)
            taskDao.upsertTasks(dtos.map { it.toEntity() })
            Log.d(TAG, "refresh OK — ${dtos.size} tasks")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "refresh failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun startTask(taskId: String, photoStartUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                taskDao.updateStart(
                    taskId        = taskId,
                    status        = TaskStatus.IN_PROGRESS.name,
                    photoStartUrl = photoStartUrl,
                    startTime     = now,
                    updatedAt     = now,
                    syncPending   = 0,
                )
                try {
                    val session = sessionManager.getSession().first()
                    if (session != null && session.accessToken.isNotBlank()) {
                        val body = json.encodeToString(StartTaskBody(
                            status        = "IN_PROGRESS",
                            photoStartUrl = photoStartUrl,
                            startTime     = now,
                            updatedAt     = now,
                        ))
                        restClient.patchTask(session.accessToken, taskId, body)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "startTask Supabase sync failed, marking pending: ${e.message}")
                    taskDao.markSyncPending(taskId)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun submitTask(
        taskId: String,
        photoEndUrl: String,
        observations: String?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now().toString()
            taskDao.updateSubmit(
                taskId      = taskId,
                status      = TaskStatus.SUBMITTED.name,
                photoEndUrl = photoEndUrl,
                endTime     = now,
                observations = observations,
                updatedAt   = now,
                syncPending = 0,
            )
            try {
                val session = sessionManager.getSession().first()
                if (session != null && session.accessToken.isNotBlank()) {
                    val body = json.encodeToString(SubmitTaskBody(
                        status       = "SUBMITTED",
                        photoEndUrl  = photoEndUrl,
                        endTime      = now,
                        observations = observations,
                        updatedAt    = now,
                    ))
                    restClient.patchTask(session.accessToken, taskId, body)
                }
            } catch (e: Exception) {
                Log.w(TAG, "submitTask Supabase sync failed, marking pending: ${e.message}")
                taskDao.markSyncPending(taskId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
private data class StartTaskBody(
    val status: String,
    @SerialName("photo_start_url") val photoStartUrl: String,
    @SerialName("start_time")      val startTime: String,
    @SerialName("updated_at")      val updatedAt: String,
)

@Serializable
private data class SubmitTaskBody(
    val status: String,
    @SerialName("photo_end_url") val photoEndUrl: String,
    @SerialName("end_time")      val endTime: String,
    val observations: String?,
    @SerialName("updated_at")    val updatedAt: String,
)
