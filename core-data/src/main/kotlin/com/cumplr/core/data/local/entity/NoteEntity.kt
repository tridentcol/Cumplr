package com.cumplr.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_notes",
    indices = [Index(value = ["task_id"])],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id")     val taskId: String,
    @ColumnInfo(name = "author_id")   val authorId: String,
    @ColumnInfo(name = "author_name") val authorName: String,
    val text: String,
    @ColumnInfo(name = "created_at")  val createdAt: String,
    @ColumnInfo(name = "sync_pending") val syncPending: Boolean = false,
)
