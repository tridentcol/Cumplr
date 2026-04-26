package com.cumplr.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    val id: String,
    @SerialName("task_id")     val taskId: String,
    @SerialName("author_id")   val authorId: String,
    @SerialName("author_name") val authorName: String,
    val text: String,
    @SerialName("created_at")  val createdAt: String,
)
