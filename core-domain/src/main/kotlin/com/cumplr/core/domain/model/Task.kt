package com.cumplr.core.domain.model

import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.enums.TaskStatus

data class Task(
    val id: String,
    val companyId: String,
    val title: String,
    val description: String?,
    val location: String?,
    val assignedTo: String,
    val assignedBy: String,
    val status: TaskStatus,
    val priority: TaskPriority,
    val deadline: String?,
    val startTime: String?,
    val endTime: String?,
    val photoStartUrl: String?,
    val photoEndUrl: String?,
    val observations: String?,
    val feedback: String?,
    val rejectionReason: String?,
    val createdAt: String,
    val updatedAt: String,
    val syncPending: Boolean = false,
)
