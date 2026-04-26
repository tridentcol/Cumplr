package com.cumplr.core.data.repository

import android.util.Log
import com.cumplr.core.data.local.dao.NoteDao
import com.cumplr.core.data.local.entity.NoteEntity
import com.cumplr.core.data.remote.SupabaseRestClient
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.data.session.TokenAuthGuard
import com.cumplr.core.domain.model.Note
import com.cumplr.core.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NoteRepo"

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val restClient: SupabaseRestClient,
    private val auth: TokenAuthGuard,
    private val sessionManager: SessionManager,
) : NoteRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    override fun getNotes(taskId: String): Flow<List<Note>> =
        noteDao.getNotesByTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun addNote(taskId: String, text: String): Result<Note> =
        withContext(Dispatchers.IO) {
            runCatching {
                val session = sessionManager.getSession().first()
                    ?: error("Sin sesión activa")
                val noteId = UUID.randomUUID().toString()
                val now    = Instant.now().toString()
                val entity = NoteEntity(
                    id          = noteId,
                    taskId      = taskId,
                    authorId    = session.userId,
                    authorName  = session.name,
                    text        = text,
                    createdAt   = now,
                    syncPending = true,
                )
                noteDao.upsertNote(entity)
                try {
                    val body = json.encodeToString(NotePostBody(noteId, taskId, session.userId, session.name, text, now))
                    auth.withValidToken { token -> restClient.postNote(token, body) }
                    noteDao.clearSyncPending(noteId)
                    Log.d(TAG, "addNote synced — $noteId")
                } catch (e: Exception) {
                    Log.w(TAG, "addNote queued offline — $noteId: ${e.message}")
                }
                entity.toDomain()
            }
        }

    override suspend fun syncPending(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val pending = noteDao.getPendingSync()
            if (pending.isEmpty()) return@runCatching
            Log.d(TAG, "syncPending — ${pending.size} notes")
            for (note in pending) {
                try {
                    val body = json.encodeToString(NotePostBody(note.id, note.taskId, note.authorId, note.authorName, note.text, note.createdAt))
                    auth.withValidToken { token -> restClient.postNote(token, body) }
                    noteDao.clearSyncPending(note.id)
                } catch (e: Exception) {
                    Log.w(TAG, "syncPending note ${note.id} failed: ${e.message}")
                }
            }
        }
    }
}

private fun NoteEntity.toDomain() = Note(id, taskId, authorId, authorName, text, createdAt)

@Serializable
private data class NotePostBody(
    val id: String,
    @SerialName("task_id")     val taskId: String,
    @SerialName("author_id")   val authorId: String,
    @SerialName("author_name") val authorName: String,
    val text: String,
    @SerialName("created_at")  val createdAt: String,
)
