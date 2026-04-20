package com.cumplr.core.domain.repository

import com.cumplr.core.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getMyTasks(userId: String): Flow<List<Task>>
    fun getTask(taskId: String): Flow<Task?>
    suspend fun refresh(userId: String): Result<Unit>
    suspend fun startTask(taskId: String, photoStartUrl: String): Result<Unit>
    suspend fun submitTask(taskId: String, photoEndUrl: String, observations: String?): Result<Unit>
}
