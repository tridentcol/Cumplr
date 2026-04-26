package com.cumplr.core.domain.model

data class Note(
    val id: String,
    val taskId: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAt: String,
)
