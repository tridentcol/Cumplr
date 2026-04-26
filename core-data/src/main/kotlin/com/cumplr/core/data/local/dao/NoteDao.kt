package com.cumplr.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cumplr.core.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Upsert
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Upsert
    suspend fun upsertNote(note: NoteEntity)

    @Query("SELECT * FROM task_notes WHERE task_id = :taskId ORDER BY created_at ASC")
    fun getNotesByTask(taskId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM task_notes WHERE sync_pending = 1")
    suspend fun getPendingSync(): List<NoteEntity>

    @Query("UPDATE task_notes SET sync_pending = 0 WHERE id = :noteId")
    suspend fun clearSyncPending(noteId: String)

    @Query("DELETE FROM task_notes WHERE task_id = :taskId")
    suspend fun deleteNotesByTask(taskId: String)
}
