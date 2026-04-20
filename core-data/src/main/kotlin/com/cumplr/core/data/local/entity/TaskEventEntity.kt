package com.cumplr.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_events")
data class TaskEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val status: String,
    val timestamp: String,
    val note: String?,
    @ColumnInfo(name = "sync_pending") val syncPending: Boolean = false,
)
