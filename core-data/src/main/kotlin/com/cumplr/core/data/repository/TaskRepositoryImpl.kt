package com.cumplr.core.data.repository

import android.util.Log
import com.cumplr.core.data.local.dao.NotificationDao
import com.cumplr.core.data.local.dao.TaskDao
import com.cumplr.core.data.local.entity.NotificationEntity
import com.cumplr.core.data.local.mapper.toDomain
import com.cumplr.core.data.local.mapper.toEntity
import com.cumplr.core.data.remote.SupabaseRealtimeClient
import com.cumplr.core.data.remote.SupabaseRestClient
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TaskRepo"

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val notificationDao: NotificationDao,
    private val restClient: SupabaseRestClient,
    private val sessionManager: SessionManager,
    private val realtimeClient: SupabaseRealtimeClient,
) : TaskRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Worker queries ────────────────────────────────────────────────────────

    override fun getMyTasks(userId: String): Flow<List<Task>> =
        taskDao.getTasksByAssignedTo(userId).map { list -> list.map { it.toDomain() } }

    override fun getTask(taskId: String): Flow<Task?> =
        taskDao.getTaskById(taskId).map { it?.toDomain() }

    override suspend fun refresh(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getSession().first()
                ?: return@withContext Result.failure(Exception("Sin sesión activa"))

            // Snapshot current DB state to detect transitions
            val existing = taskDao.getTasksByAssignedTo(userId).first().associateBy { it.id }

            val dtos = restClient.getTasks(session.accessToken, userId)
            taskDao.upsertTasks(dtos.map { it.toEntity() })
            Log.d(TAG, "refresh OK — ${dtos.size} tasks")

            // Generate local notifications for status transitions detected from the server
            val now = Instant.now().toString()
            val newNotifs = dtos.mapNotNull { dto ->
                val old = existing[dto.id]
                when {
                    old == null && existing.isNotEmpty() ->
                        // Brand-new task assigned while the app was running (skip on first-ever load)
                        NotificationEntity(
                            id        = UUID.randomUUID().toString(),
                            userId    = userId,
                            companyId = dto.companyId,
                            type      = "TASK_ASSIGNED",
                            taskId    = dto.id,
                            title     = "Nueva tarea asignada",
                            body      = "Se te asignó \"${dto.title}\"",
                            read      = false,
                            createdAt = now,
                        )
                    old != null && old.status != "APPROVED" && dto.status == "APPROVED" ->
                        NotificationEntity(
                            id        = UUID.randomUUID().toString(),
                            userId    = userId,
                            companyId = dto.companyId,
                            type      = "TASK_APPROVED",
                            taskId    = dto.id,
                            title     = "Tarea aprobada ✓",
                            body      = "\"${dto.title}\" fue aprobada." +
                                if (!dto.feedback.isNullOrBlank()) " ${dto.feedback}" else "",
                            read      = false,
                            createdAt = now,
                        )
                    old != null && old.status != "REJECTED" && dto.status == "REJECTED" ->
                        NotificationEntity(
                            id        = UUID.randomUUID().toString(),
                            userId    = userId,
                            companyId = dto.companyId,
                            type      = "TASK_REJECTED",
                            taskId    = dto.id,
                            title     = "Tarea rechazada",
                            body      = "\"${dto.title}\" fue rechazada." +
                                if (!dto.rejectionReason.isNullOrBlank()) " Motivo: ${dto.rejectionReason}" else "",
                            read      = false,
                            createdAt = now,
                        )
                    else -> null
                }
            }
            if (newNotifs.isNotEmpty()) {
                notificationDao.upsertNotifications(newNotifs)
                Log.d(TAG, "refresh — generated ${newNotifs.size} notifications")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "refresh failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun startTask(taskId: String, photoStartUrl: String, startTime: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                taskDao.updateStart(
                    taskId        = taskId,
                    status        = TaskStatus.IN_PROGRESS.name,
                    photoStartUrl = photoStartUrl,
                    startTime     = startTime,
                    updatedAt     = now,
                    syncPending   = 0,
                )
                try {
                    val session = sessionManager.getSession().first()
                    if (session != null && session.accessToken.isNotBlank()) {
                        val body = json.encodeToString(StartTaskBody("IN_PROGRESS", photoStartUrl, startTime, now))
                        restClient.patchTask(session.accessToken, taskId, body)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "startTask sync failed: ${e.message}")
                    taskDao.markSyncPending(taskId)
                }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    override suspend fun submitTask(
        taskId: String,
        photoEndUrl: String,
        observations: String?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now().toString()
            taskDao.updateSubmit(
                taskId       = taskId,
                status       = TaskStatus.SUBMITTED.name,
                photoEndUrl  = photoEndUrl,
                endTime      = now,
                observations = observations,
                updatedAt    = now,
                syncPending  = 0,
            )
            try {
                val session = sessionManager.getSession().first()
                if (session != null && session.accessToken.isNotBlank()) {
                    val body = json.encodeToString(SubmitTaskBody("SUBMITTED", photoEndUrl, now, observations, now))
                    restClient.patchTask(session.accessToken, taskId, body)
                }
            } catch (e: Exception) {
                Log.w(TAG, "submitTask sync failed: ${e.message}")
                taskDao.markSyncPending(taskId)
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Chief queries ─────────────────────────────────────────────────────────

    override fun getTasksByCompany(companyId: String): Flow<List<Task>> =
        taskDao.getTasksByCompany(companyId).map { list -> list.map { it.toDomain() } }

    override fun getActiveTasksCount(companyId: String): Flow<Int> =
        taskDao.getActiveTasksCount(companyId)

    override fun getCompletionRate(companyId: String): Flow<Float> =
        taskDao.getCompletionRate(companyId)

    override fun getOverdueCount(companyId: String): Flow<Int> =
        taskDao.getOverdueCount(companyId)

    override fun getTasksForReview(companyId: String): Flow<List<Task>> =
        taskDao.getTasksForReview(companyId).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshCompanyTasks(companyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getSession().first()
                ?: return@withContext Result.failure(Exception("Sin sesión activa"))
            val dtos = restClient.getCompanyTasks(session.accessToken, companyId)
            taskDao.upsertTasks(dtos.map { it.toEntity() })
            Log.d(TAG, "refreshCompanyTasks OK — ${dtos.size}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "refreshCompanyTasks failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun approveTask(taskId: String, feedback: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                taskDao.updateApproval(taskId, TaskStatus.APPROVED.name, feedback, now, 0)
                val task = taskDao.getTaskById(taskId).first()
                if (task != null) {
                    notificationDao.upsertNotifications(listOf(
                        NotificationEntity(
                            id        = UUID.randomUUID().toString(),
                            userId    = task.assignedTo,
                            companyId = task.companyId,
                            type      = "TASK_APPROVED",
                            taskId    = taskId,
                            title     = "Tarea aprobada",
                            body      = "\"${task.title}\" fue aprobada." +
                                if (!feedback.isNullOrBlank()) " Feedback: $feedback" else "",
                            read      = false,
                            createdAt = now,
                        )
                    ))
                }
                try {
                    val session = sessionManager.getSession().first()
                    if (session?.accessToken?.isNotBlank() == true) {
                        val body = json.encodeToString(ApproveTaskBody("APPROVED", feedback, now))
                        restClient.patchTask(session.accessToken, taskId, body)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "approveTask sync failed: ${e.message}")
                    taskDao.markSyncPending(taskId)
                }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    override suspend fun rejectTask(taskId: String, rejectionReason: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                taskDao.updateRejection(taskId, TaskStatus.REJECTED.name, rejectionReason, now, 0)
                val task = taskDao.getTaskById(taskId).first()
                if (task != null) {
                    notificationDao.upsertNotifications(listOf(
                        NotificationEntity(
                            id        = UUID.randomUUID().toString(),
                            userId    = task.assignedTo,
                            companyId = task.companyId,
                            type      = "TASK_REJECTED",
                            taskId    = taskId,
                            title     = "Tarea rechazada",
                            body      = "\"${task.title}\" fue rechazada. Motivo: $rejectionReason",
                            read      = false,
                            createdAt = now,
                        )
                    ))
                }
                try {
                    val session = sessionManager.getSession().first()
                    if (session?.accessToken?.isNotBlank() == true) {
                        val body = json.encodeToString(RejectTaskBody("REJECTED", rejectionReason, now))
                        restClient.patchTask(session.accessToken, taskId, body)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "rejectTask sync failed: ${e.message}")
                    taskDao.markSyncPending(taskId)
                }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    override suspend fun reopenTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now().toString()
            taskDao.reopenTask(taskId, TaskStatus.IN_PROGRESS.name, now, 0)
            try {
                val session = sessionManager.getSession().first()
                if (session?.accessToken?.isNotBlank() == true) {
                    val body = json.encodeToString(ReopenTaskBody(TaskStatus.IN_PROGRESS.name, now))
                    restClient.patchTask(session.accessToken, taskId, body)
                }
            } catch (e: Exception) {
                Log.w(TAG, "reopenTask sync failed: ${e.message}")
                taskDao.markSyncPending(taskId)
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun markTaskStarted(taskId: String, startTime: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                taskDao.markTaskStarted(taskId, TaskStatus.IN_PROGRESS.name, startTime, now, 0)
                try {
                    val session = sessionManager.getSession().first()
                    if (session?.accessToken?.isNotBlank() == true) {
                        val body = json.encodeToString(MarkStartedBody(TaskStatus.IN_PROGRESS.name, startTime, now))
                        restClient.patchTask(session.accessToken, taskId, body)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "markTaskStarted sync failed: ${e.message}")
                    taskDao.markSyncPending(taskId)
                }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    // ── Realtime ──────────────────────────────────────────────────────────────

    override fun startRealtimeForWorker(userId: String) {
        repoScope.launch {
            val session = sessionManager.getSession().first() ?: return@launch
            realtimeClient.connect(
                accessToken = session.accessToken,
                filter      = "assigned_to=eq.$userId",
                onTask      = { dto -> repoScope.launch { handleRealtimeTask(dto, userId) } },
            )
        }
    }

    override fun startRealtimeForChief(companyId: String) {
        repoScope.launch {
            val session = sessionManager.getSession().first() ?: return@launch
            realtimeClient.connect(
                accessToken = session.accessToken,
                filter      = "company_id=eq.$companyId",
                onTask      = { dto -> repoScope.launch { taskDao.upsertTask(dto.toEntity()) } },
            )
        }
    }

    override fun stopRealtime() = realtimeClient.disconnect()

    private suspend fun handleRealtimeTask(dto: TaskDto, userId: String) {
        val existing = taskDao.getTaskById(dto.id).first()
        taskDao.upsertTask(dto.toEntity())

        val now   = Instant.now().toString()
        val notif = when {
            existing == null && dto.status == "ASSIGNED" ->
                NotificationEntity(
                    id        = UUID.randomUUID().toString(),
                    userId    = userId,
                    companyId = dto.companyId,
                    type      = "TASK_ASSIGNED",
                    taskId    = dto.id,
                    title     = "Nueva tarea asignada",
                    body      = "Se te asignó \"${dto.title}\"",
                    read      = false,
                    createdAt = now,
                )
            existing != null && existing.status != "APPROVED" && dto.status == "APPROVED" ->
                NotificationEntity(
                    id        = UUID.randomUUID().toString(),
                    userId    = userId,
                    companyId = dto.companyId,
                    type      = "TASK_APPROVED",
                    taskId    = dto.id,
                    title     = "Tarea aprobada ✓",
                    body      = "\"${dto.title}\" fue aprobada." +
                        if (!dto.feedback.isNullOrBlank()) " ${dto.feedback}" else "",
                    read      = false,
                    createdAt = now,
                )
            existing != null && existing.status != "REJECTED" && dto.status == "REJECTED" ->
                NotificationEntity(
                    id        = UUID.randomUUID().toString(),
                    userId    = userId,
                    companyId = dto.companyId,
                    type      = "TASK_REJECTED",
                    taskId    = dto.id,
                    title     = "Tarea rechazada",
                    body      = "\"${dto.title}\" fue rechazada." +
                        if (!dto.rejectionReason.isNullOrBlank()) " Motivo: ${dto.rejectionReason}" else "",
                    read      = false,
                    createdAt = now,
                )
            else -> null
        }
        notif?.let { notificationDao.upsertNotifications(listOf(it)) }
    }

    override suspend fun updateTask(
        taskId: String,
        title: String,
        description: String?,
        location: String?,
        deadline: String?,
        assignedTo: String,
        priority: TaskPriority,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now().toString()
            taskDao.updateTask(taskId, title, description, location, deadline, assignedTo, priority.name, now, 0)
            try {
                val session = sessionManager.getSession().first()
                if (session?.accessToken?.isNotBlank() == true) {
                    val body = json.encodeToString(
                        UpdateTaskBody(title, description, location, assignedTo, deadline, priority.name, now)
                    )
                    restClient.patchTask(session.accessToken, taskId, body)
                }
            } catch (e: Exception) {
                Log.w(TAG, "updateTask sync failed: ${e.message}")
                taskDao.markSyncPending(taskId)
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun createTask(
        title: String,
        description: String?,
        location: String?,
        assignedTo: String,
        deadline: String?,
        priority: TaskPriority,
        companyId: String,
        assignedBy: String,
    ): Result<Task> = withContext(Dispatchers.IO) {
        try {
            val now    = Instant.now().toString()
            val taskId = UUID.randomUUID().toString()
            val entity = com.cumplr.core.data.local.entity.TaskEntity(
                id              = taskId,
                companyId       = companyId,
                title           = title,
                description     = description,
                location        = location,
                assignedTo      = assignedTo,
                assignedBy      = assignedBy,
                status          = TaskStatus.ASSIGNED.name,
                priority        = priority.name,
                deadline        = deadline,
                startTime       = null,
                endTime         = null,
                photoStartUrl   = null,
                photoEndUrl     = null,
                observations    = null,
                feedback        = null,
                rejectionReason = null,
                createdAt       = now,
                updatedAt       = now,
                syncPending     = false,
            )
            taskDao.upsertTask(entity)
            val task = entity.toDomain()
            try {
                val session = sessionManager.getSession().first()
                if (session?.accessToken?.isNotBlank() == true) {
                    val body = json.encodeToString(
                        CreateTaskBody(taskId, companyId, title, description, location, assignedTo, assignedBy, priority.name, deadline, now, now)
                    )
                    restClient.postTask(session.accessToken, body)
                }
            } catch (e: Exception) {
                Log.w(TAG, "createTask sync failed: ${e.message}")
                taskDao.markSyncPending(taskId)
            }
            Result.success(task)
        } catch (e: Exception) { Result.failure(e) }
    }
}

@Serializable private data class StartTaskBody(
    val status: String,
    @SerialName("photo_start_url") val photoStartUrl: String,
    @SerialName("start_time")      val startTime: String,
    @SerialName("updated_at")      val updatedAt: String,
)

@Serializable private data class SubmitTaskBody(
    val status: String,
    @SerialName("photo_end_url") val photoEndUrl: String,
    @SerialName("end_time")      val endTime: String,
    val observations: String?,
    @SerialName("updated_at")    val updatedAt: String,
)

@Serializable private data class ApproveTaskBody(
    val status: String,
    val feedback: String?,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable private data class RejectTaskBody(
    val status: String,
    @SerialName("rejection_reason") val rejectionReason: String,
    @SerialName("updated_at")       val updatedAt: String,
)

@Serializable private data class CreateTaskBody(
    val id: String,
    @SerialName("company_id")  val companyId: String,
    val title: String,
    val description: String?,
    val location: String?,
    @SerialName("assigned_to") val assignedTo: String,
    @SerialName("assigned_by") val assignedBy: String,
    val priority: String,
    val deadline: String?,
    @SerialName("created_at")  val createdAt: String,
    @SerialName("updated_at")  val updatedAt: String,
    val status: String = "ASSIGNED",
)

@Serializable private data class ReopenTaskBody(
    val status: String,
    @SerialName("updated_at")    val updatedAt: String,
    @SerialName("photo_end_url") val photoEndUrl: String? = null,
    @SerialName("end_time")      val endTime: String? = null,
)

@Serializable private data class MarkStartedBody(
    val status: String,
    @SerialName("start_time")  val startTime: String,
    @SerialName("updated_at")  val updatedAt: String,
)

@Serializable private data class UpdateTaskBody(
    val title: String,
    val description: String?,
    val location: String?,
    @SerialName("assigned_to") val assignedTo: String,
    val deadline: String?,
    val priority: String,
    @SerialName("updated_at")  val updatedAt: String,
)
