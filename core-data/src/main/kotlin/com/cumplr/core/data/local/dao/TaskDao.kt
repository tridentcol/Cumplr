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
        syncPending: Boolean,
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
        syncPending: Boolean,
    )

    @Query("UPDATE tasks SET sync_pending = 1 WHERE id = :taskId")
    suspend fun markSyncPending(taskId: String)
}
