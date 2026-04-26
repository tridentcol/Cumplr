package com.cumplr.core.domain.repository

import com.cumplr.core.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(taskId: String): Flow<List<Note>>
    suspend fun addNote(taskId: String, text: String): Result<Note>
    suspend fun syncPending(): Result<Unit>
}
