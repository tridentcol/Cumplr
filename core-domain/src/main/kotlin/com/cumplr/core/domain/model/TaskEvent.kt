package com.cumplr.core.domain.model

import com.cumplr.core.domain.enums.TaskStatus

data class TaskEvent(
    val id: String,
    val taskId: String,
    val userId: String,
    val status: TaskStatus,
    val timestamp: String,
    val note: String?
)
