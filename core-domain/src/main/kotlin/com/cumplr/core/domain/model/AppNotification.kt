package com.cumplr.core.domain.model

data class AppNotification(
    val id: String,
    val userId: String,
    val companyId: String,
    val type: String,
    val taskId: String?,
    val title: String,
    val body: String,
    val read: Boolean,
    val createdAt: String
)
