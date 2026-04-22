package com.cumplr.core.domain.repository

import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getMyTasks(userId: String): Flow<List<Task>>
    fun getTask(taskId: String): Flow<Task?>
    fun getTasksByCompany(companyId: String): Flow<List<Task>>
    fun getActiveTasksCount(companyId: String): Flow<Int>
    fun getCompletionRate(companyId: String): Flow<Float>
    fun getOverdueCount(companyId: String): Flow<Int>
    fun getTasksForReview(companyId: String): Flow<List<Task>>
    suspend fun refresh(userId: String): Result<Unit>
    suspend fun refreshCompanyTasks(companyId: String): Result<Unit>
    suspend fun startTask(taskId: String, photoStartUrl: String, startTime: String): Result<Unit>
    suspend fun submitTask(taskId: String, photoEndUrl: String, observations: String?): Result<Unit>
    suspend fun approveTask(taskId: String, feedback: String?): Result<Unit>
    suspend fun rejectTask(taskId: String, rejectionReason: String): Result<Unit>
    suspend fun reopenTask(taskId: String): Result<Unit>
    suspend fun markTaskStarted(taskId: String, startTime: String): Result<Unit>
    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String?,
        location: String?,
        deadline: String?,
        assignedTo: String,
        priority: TaskPriority,
    ): Result<Unit>
    suspend fun createTask(
        title: String,
        description: String?,
        location: String?,
        assignedTo: String,
        deadline: String?,
        priority: TaskPriority,
        companyId: String,
        assignedBy: String,
    ): Result<Task>
}
