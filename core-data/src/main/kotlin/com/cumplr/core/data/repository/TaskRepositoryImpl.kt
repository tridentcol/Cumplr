package com.cumplr.core.data.repository

import android.util.Log
import com.cumplr.core.data.local.dao.NotificationDao
import com.cumplr.core.data.local.dao.TaskDao
import com.cumplr.core.data.local.entity.NotificationEntity
import com.cumplr.core.data.local.mapper.toDomain
import com.cumplr.core.data.local.mapper.toEntity
import com.cumplr.core.data.remote.SupabaseRealtimeClient
import com.cumplr.core.data.remote.SupabaseRestClient
import com.cumplr.core.data.remote.dto.TaskDto
import com.cumplr.core.data.session.TokenAuthGuard
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
    private val realtimeClient: SupabaseRealtimeClient,
    private val auth: TokenAuthGuard,
) : TaskRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Re-POST tasks that were created locally but never reached Supabase.
     * Filtered to truly-new rows (status=ASSIGNED, no times) so we never
     * overwrite worker progress on a row whose state has moved on.
     * Server uses plain INSERT; 409 is treated as idempotent success in
     * SupabaseRestClient.postTask().
     */
    private suspend fun retrySyncPendingCreates() {
        val pending = taskDao.getPendingSyncByOp("CREATE")
        if (pending.isEmpty()) return
        Log.d(TAG, "retrySyncPendingCreates — ${pending.size} unposted tasks")
        for (entity in pending) {
            try {
                val body = json.encodeToString(
                    CreateTaskBody(
                        id          = entity.id,
                        companyId   = entity.companyId,
                        title       = entity.title,
                        description = entity.description,
                        location    = entity.location,
                        assignedTo  = entity.assignedTo,
                        assignedBy  = entity.assignedBy,
                        status      = entity.status,
                        priority    = entity.priority,
                        deadline    = entity.deadline,
                        createdAt   = entity.createdAt,
                        updatedAt   = entity.updatedAt,
                    )
                )
                auth.withValidToken { token -> restClient.postTask(token, body) }
                taskDao.clearSync(entity.id)
                Log.d(TAG, "retrySyncPendingCreates — posted ${entity.id}")
            } catch (e: Exception) {
                Log.w(TAG, "retrySyncPendingCreates — still failing ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun retrySyncPendingPatches() {
        val pending = taskDao.getPendingSyncByOp("PATCH")
        if (pending.isEmpty()) return
        Log.d(TAG, "retrySyncPendingPatches — ${pending.size} unsynced PATCHes")
        for (entity in pending) {
            try {
                val body = json.encodeToString(
                    FullStatePatchBody(
                        status          = entity.status,
                        title           = entity.title,
                        description     = entity.description,
                        location        = entity.location,
                        assignedTo      = entity.assignedTo,
                        deadline        = entity.deadline,
                        priority        = entity.priority,
                        photoStartUrl   = entity.photoStartUrl,
                        photoEndUrl     = entity.photoEndUrl,
                        startTime       = entity.startTime,
                        endTime         = entity.endTime,
                        observations    = entity.observations,
                        feedback        = entity.feedback,
                        rejectionReason = entity.rejectionReason,
                        updatedAt       = entity.updatedAt,
                    )
                )
                auth.withValidToken { token -> restClient.patchTask(token, entity.id, body) }
                taskDao.clearSync(entity.id)
                Log.d(TAG, "retrySyncPendingPatches — synced ${entity.id}")
            } catch (e: Exception) {
                Log.w(TAG, "retrySyncPendingPatches — still failing ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun pruneOldCompletedTasks() {
        val cutoff = Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS).toString()
        taskDao.deleteCompletedBefore(cutoff)
    }

    // ── Worker queries ────────────────────────────────────────────────────────

    override fun getMyTasks(userId: String): Flow<List<Task>> =
        taskDao.getTasksByAssignedTo(userId).map { list -> list.map { it.toDomain() } }

    override fun getTask(taskId: String): Flow<Task?> =
        taskDao.getTaskById(taskId).map { it?.toDomain() }

    override suspend fun refresh(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Snapshot current DB state BEFORE the network call so we can detect transitions.
            val existing = taskDao.getTasksByAssignedTo(userId).first().associateBy { it.id }

            val dtos = auth.withValidToken { token -> restClient.getTasks(token, userId) }
            taskDao.upsertTasks(dtos.map { it.toEntity() })
            Log.d(TAG, "refresh OK — ${dtos.size} tasks")

            // Delete local tasks that no longer exist on server (deleted or reassigned by chief).
            // Never delete rows with pendingSyncOp == "CREATE" — those haven't reached the server yet.
            val remoteIds = dtos.map { it.id }.toSet()
            val orphanIds = existing.keys.filter { it !in remoteIds && existing[it]?.pendingSyncOp != "CREATE" }
            if (orphanIds.isNotEmpty()) {
                orphanIds.forEach { taskDao.deleteTask(it) }
                Log.d(TAG, "refresh — deleted ${orphanIds.size} orphaned tasks")
            }

            // Generate local notifications for status transitions detected from the server.
            val now = Instant.now().toString()
            val newNotifs = dtos.mapNotNull { dto ->
                val old = existing[dto.id]
                when {
                    old == null && existing.isNotEmpty() ->
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
        }.onFailure { Log.w(TAG, "refresh failed: ${it.message}") }
    }

    override suspend fun startTask(taskId: String, photoStartUrl: String, startTime: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
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
                    val body = json.encodeToString(StartTaskBody("IN_PROGRESS", photoStartUrl, startTime, now))
                    auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
                } catch (e: Exception) {
                    Log.w(TAG, "startTask sync failed: ${e.message}")
                    taskDao.markPending(taskId, "PATCH")
                }
            }
        }

    override suspend fun submitTask(
        taskId: String,
        photoEndUrl: String,
        observations: String?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
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
                val body = json.encodeToString(SubmitTaskBody("SUBMITTED", photoEndUrl, now, observations, now))
                auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
            } catch (e: Exception) {
                Log.w(TAG, "submitTask sync failed: ${e.message}")
                taskDao.markPending(taskId, "PATCH")
            }
        }
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
        runCatching {
            val dtos = auth.withValidToken { token -> restClient.getCompanyTasks(token, companyId) }

            // Only upsert rows that changed on the server, and never overwrite rows
            // that have local mutations not yet sent (pendingSyncOp != null).
            val localById = taskDao.getTasksByCompanySnapshot(companyId).associateBy { it.id }
            val toUpsert = dtos.filter { dto ->
                val local = localById[dto.id]
                local == null || (local.updatedAt < dto.updatedAt && local.pendingSyncOp == null)
            }
            if (toUpsert.isNotEmpty()) taskDao.upsertTasks(toUpsert.map { it.toEntity() })
            Log.d(TAG, "refreshCompanyTasks OK — ${dtos.size} server, ${toUpsert.size} upserted")

            retrySyncPendingCreates()
            retrySyncPendingPatches()
            pruneOldCompletedTasks()
        }.onFailure { Log.w(TAG, "refreshCompanyTasks failed: ${it.message}") }
    }

    override suspend fun approveTask(taskId: String, feedback: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
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
                    val body = json.encodeToString(ApproveTaskBody("APPROVED", feedback, now))
                    auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
                } catch (e: Exception) {
                    Log.w(TAG, "approveTask sync failed: ${e.message}")
                    taskDao.markPending(taskId, "PATCH")
                }
            }
        }

    override suspend fun rejectTask(taskId: String, rejectionReason: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
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
                    val body = json.encodeToString(RejectTaskBody("REJECTED", rejectionReason, now))
                    auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
                } catch (e: Exception) {
                    Log.w(TAG, "rejectTask sync failed: ${e.message}")
                    taskDao.markPending(taskId, "PATCH")
                }
            }
        }

    override suspend fun reopenTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val now = Instant.now().toString()
            taskDao.reopenTask(taskId, TaskStatus.IN_PROGRESS.name, now, 0)
            try {
                val body = json.encodeToString(ReopenTaskBody(TaskStatus.IN_PROGRESS.name, now))
                auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
            } catch (e: Exception) {
                Log.w(TAG, "reopenTask sync failed: ${e.message}")
                taskDao.markPending(taskId, "PATCH")
            }
        }
    }

    override suspend fun markTaskStarted(taskId: String, startTime: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val now = Instant.now().toString()
                taskDao.markTaskStarted(taskId, TaskStatus.IN_PROGRESS.name, startTime, now, 0)
                try {
                    val body = json.encodeToString(MarkStartedBody(TaskStatus.IN_PROGRESS.name, startTime, now))
                    auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
                } catch (e: Exception) {
                    Log.w(TAG, "markTaskStarted sync failed: ${e.message}")
                    taskDao.markPending(taskId, "PATCH")
                }
            }
        }

    // ── Realtime ──────────────────────────────────────────────────────────────

    override fun startRealtimeForWorker(userId: String) {
        repoScope.launch {
            val token = auth.freshTokenForRealtime() ?: return@launch
            realtimeClient.connect(
                accessToken = token,
                filter      = "assigned_to=eq.$userId",
                onTask      = { dto -> repoScope.launch { handleRealtimeTask(dto, userId) } },
            )
        }
    }

    override fun startRealtimeForChief(companyId: String) {
        repoScope.launch {
            val token = auth.freshTokenForRealtime() ?: return@launch
            realtimeClient.connect(
                accessToken = token,
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
        runCatching {
            val now = Instant.now().toString()
            taskDao.updateTask(taskId, title, description, location, deadline, assignedTo, priority.name, now, 0)
            try {
                val body = json.encodeToString(
                    UpdateTaskBody(title, description, location, assignedTo, deadline, priority.name, now)
                )
                auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
            } catch (e: Exception) {
                Log.w(TAG, "updateTask sync failed: ${e.message}")
                taskDao.markPending(taskId, "PATCH")
            }
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            taskDao.deleteTask(taskId)
            auth.withValidToken { token -> restClient.deleteTask(token, taskId) }
            Log.d(TAG, "deleteTask OK — $taskId")
            Unit
        }
    }

    override suspend fun markUnderReview(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val now = Instant.now().toString()
            taskDao.markUnderReview(taskId, now)
            try {
                val body = json.encodeToString(UnderReviewBody("UNDER_REVIEW", now))
                auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
                taskDao.clearSync(taskId)
                Log.d(TAG, "markUnderReview OK — $taskId")
            } catch (e: Exception) {
                Log.w(TAG, "markUnderReview sync failed: ${e.message}")
            }
            Unit
        }
    }

    override suspend fun reassignTask(taskId: String, newAssignedTo: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val now = Instant.now().toString()
                taskDao.updateAssignedTo(taskId, newAssignedTo, now)
                taskDao.markPending(taskId, "PATCH")
                try {
                    val body = json.encodeToString(ReassignBody(newAssignedTo, now))
                    auth.withValidToken { token -> restClient.patchTask(token, taskId, body) }
                    taskDao.clearSync(taskId)
                    Log.d(TAG, "reassignTask OK — taskId=$taskId newAssignee=$newAssignedTo")
                } catch (e: Exception) {
                    Log.w(TAG, "reassignTask sync failed: ${e.message}")
                }
                Unit
            }
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
        runCatching {
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
                syncPending     = true,
                pendingSyncOp   = "CREATE",
            )
            taskDao.upsertTask(entity)
            val task = entity.toDomain()
            try {
                val body = json.encodeToString(
                    CreateTaskBody(taskId, companyId, title, description, location, assignedTo, assignedBy, "ASSIGNED", priority.name, deadline, now, now)
                )
                auth.withValidToken { token -> restClient.postTask(token, body) }
                taskDao.clearSync(taskId)
                Log.d(TAG, "createTask posted to Supabase — taskId=$taskId")
            } catch (e: Exception) {
                Log.w(TAG, "createTask sync failed, queued for retry: ${e.message}")
            }
            task
        }
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
    val status: String,
    val priority: String,
    val deadline: String?,
    @SerialName("created_at")  val createdAt: String,
    @SerialName("updated_at")  val updatedAt: String,
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

@Serializable private data class UnderReviewBody(
    val status: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable private data class ReassignBody(
    @SerialName("assigned_to") val assignedTo: String,
    @SerialName("updated_at")  val updatedAt: String,
)

@Serializable private data class FullStatePatchBody(
    val status: String,
    val title: String,
    val description: String?,
    val location: String?,
    @SerialName("assigned_to")      val assignedTo: String,
    val deadline: String?,
    val priority: String,
    @SerialName("photo_start_url")  val photoStartUrl: String?,
    @SerialName("photo_end_url")    val photoEndUrl: String?,
    @SerialName("start_time")       val startTime: String?,
    @SerialName("end_time")         val endTime: String?,
    val observations: String?,
    val feedback: String?,
    @SerialName("rejection_reason") val rejectionReason: String?,
    @SerialName("updated_at")       val updatedAt: String,
)
