package com.cumplr.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cumplr.core.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE assigned_to = :userId ORDER BY deadline ASC")
    fun getTasksByAssignedTo(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE company_id = :companyId ORDER BY deadline ASC")
    fun getTasksByCompany(companyId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE company_id = :companyId AND status = :status ORDER BY deadline ASC")
    fun getTasksByStatus(companyId: String, status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE sync_pending = 1")
    suspend fun getPendingSync(): List<TaskEntity>

    @Query("UPDATE tasks SET status = :status, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateStatus(taskId: String, status: String, updatedAt: String)

    @Query("""
        UPDATE tasks
        SET status = :status,
            photo_start_url = :photoStartUrl,
            start_time = :startTime,
            updated_at = :updatedAt,
            sync_pending = :syncPending
        WHERE id = :taskId
    """)
    suspend fun updateStart(
        taskId: String,
        status: String,
        photoStartUrl: String,
        startTime: String,
        updatedAt: String,
        syncPending: Int,
    )

    @Query("""
        UPDATE tasks
        SET status = :status,
            photo_end_url = :photoEndUrl,
            end_time = :endTime,
            observations = :observations,
            updated_at = :updatedAt,
            sync_pending = :syncPending
        WHERE id = :taskId
    """)
    suspend fun updateSubmit(
        taskId: String,
        status: String,
        photoEndUrl: String,
        endTime: String,
        observations: String?,
        updatedAt: String,
        syncPending: Int,
    )

    @Query("UPDATE tasks SET sync_pending = 1 WHERE id = :taskId")
    suspend fun markSyncPending(taskId: String)

    @Query("UPDATE tasks SET sync_pending = 0 WHERE id = :taskId")
    suspend fun clearSyncPending(taskId: String)

    @Query("""
        UPDATE tasks
        SET status = :status,
            photo_end_url = NULL,
            end_time = NULL,
            updated_at = :updatedAt,
            sync_pending = :syncPending
        WHERE id = :taskId
    """)
    suspend fun reopenTask(taskId: String, status: String, updatedAt: String, syncPending: Int)

    @Query("""
        UPDATE tasks
        SET status = :status,
            start_time = :startTime,
            updated_at = :updatedAt,
            sync_pending = :syncPending
        WHERE id = :taskId
    """)
    suspend fun markTaskStarted(taskId: String, status: String, startTime: String, updatedAt: String, syncPending: Int)

    @Query("""
        UPDATE tasks
        SET title = :title,
            description = :description,
            location = :location,
            deadline = :deadline,
            assigned_to = :assignedTo,
            priority = :priority,
            updated_at = :updatedAt,
            sync_pending = :syncPending
        WHERE id = :taskId
    """)
    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String?,
        location: String?,
        deadline: String?,
        assignedTo: String,
        priority: String,
        updatedAt: String,
        syncPending: Int,
    )

    // ── Chief metrics ─────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM tasks WHERE company_id = :companyId AND status IN ('ASSIGNED', 'IN_PROGRESS')")
    fun getActiveTasksCount(companyId: String): Flow<Int>

    @Query("""
        SELECT CASE WHEN COUNT(*) = 0 THEN 0.0
        ELSE CAST(SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)
        END FROM tasks WHERE company_id = :companyId
    """)
    fun getCompletionRate(companyId: String): Flow<Float>

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE company_id = :companyId
        AND status NOT IN ('APPROVED', 'REJECTED')
        AND deadline IS NOT NULL
        AND deadline < strftime('%Y-%m-%dT%H:%M:%SZ', 'now')
    """)
    fun getOverdueCount(companyId: String): Flow<Int>

    @Query("SELECT * FROM tasks WHERE company_id = :companyId AND status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY updated_at DESC")
    fun getTasksForReview(companyId: String): Flow<List<TaskEntity>>

    // ── Chief approval/rejection ───────────────────────────────────────────────

    @Query("""
        UPDATE tasks
        SET status = :status,
            feedback = :feedback,
            updated_at = :updatedAt,
            sync_pending = :syncPending
        WHERE id = :taskId
    """)
    suspend fun updateApproval(taskId: String, status: String, feedback: String?, updatedAt: String, syncPending: Int)

    @Query("""
        UPDATE tasks
        SET status = :status,
            rejection_reason = :rejectionReason,
            updated_at = :updatedAt,
            sync_pending = :syncPending
        WHERE id = :taskId
    """)
    suspend fun updateRejection(taskId: String, status: String, rejectionReason: String, updatedAt: String, syncPending: Int)

    // ── Nuevos métodos v2 ─────────────────────────────────────────────────────

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("""
        UPDATE tasks
        SET status = 'UNDER_REVIEW',
            updated_at = :updatedAt,
            sync_pending = 1,
            pending_sync_op = 'PATCH'
        WHERE id = :taskId
    """)
    suspend fun markUnderReview(taskId: String, updatedAt: String)

    @Query("DELETE FROM tasks WHERE status IN ('APPROVED', 'REJECTED') AND updated_at < :cutoff")
    suspend fun deleteCompletedBefore(cutoff: String)

    @Query("UPDATE tasks SET sync_pending = 1, pending_sync_op = :op WHERE id = :taskId")
    suspend fun markPending(taskId: String, op: String)

    @Query("UPDATE tasks SET assigned_to = :assignedTo, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateAssignedTo(taskId: String, assignedTo: String, updatedAt: String)

    @Query("UPDATE tasks SET sync_pending = 0, pending_sync_op = NULL WHERE id = :taskId")
    suspend fun clearSync(taskId: String)

    @Query("SELECT * FROM tasks WHERE pending_sync_op = :op")
    suspend fun getPendingSyncByOp(op: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE company_id = :companyId")
    suspend fun getTasksByCompanySnapshot(companyId: String): List<TaskEntity>
}
