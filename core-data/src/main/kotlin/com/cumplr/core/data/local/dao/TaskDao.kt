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
}
