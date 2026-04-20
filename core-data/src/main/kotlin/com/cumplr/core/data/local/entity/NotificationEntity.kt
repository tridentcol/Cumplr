package com.cumplr.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "company_id") val companyId: String,
    val type: String,
    @ColumnInfo(name = "task_id") val taskId: String?,
    val title: String,
    val body: String,
    val read: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
)
