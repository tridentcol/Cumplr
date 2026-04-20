package com.cumplr.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "company_id") val companyId: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "assigned_to") val assignedTo: String,
    @ColumnInfo(name = "assigned_by") val assignedBy: String,
    val status: String,
    val priority: String,
    val deadline: String?,
    @ColumnInfo(name = "start_time") val startTime: String?,
    @ColumnInfo(name = "end_time") val endTime: String?,
    @ColumnInfo(name = "photo_start_url") val photoStartUrl: String?,
    @ColumnInfo(name = "photo_end_url") val photoEndUrl: String?,
    val observations: String?,
    val feedback: String?,
    @ColumnInfo(name = "rejection_reason") val rejectionReason: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "sync_pending") val syncPending: Boolean = false,
)
