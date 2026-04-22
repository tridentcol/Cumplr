package com.cumplr.app.ui.worker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.data.task.TaskDraftStore
import com.cumplr.core.data.task.TaskPhotoStore
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.repository.StorageRepository
import com.cumplr.core.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class ExecutionUiState {
    object Loading   : ExecutionUiState()
    object Idle      : ExecutionUiState()
    object Uploading : ExecutionUiState()
    object Success   : ExecutionUiState()
    data class Error(val message: String) : ExecutionUiState()
}

@HiltViewModel
class TaskExecutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val storageRepository: StorageRepository,
    private val sessionManager: SessionManager,
    private val photoStore: TaskPhotoStore,
    private val draftStore: TaskDraftStore,
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _step = MutableStateFlow(1)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _observations = MutableStateFlow("")
    val observations: StateFlow<String> = _observations.asStateFlow()

    // Confirmed photos (persisted to disk)
    private val _startPhotoBytes = MutableStateFlow<ByteArray?>(null)
    val startPhotoBytes: StateFlow<ByteArray?> = _startPhotoBytes.asStateFlow()

    private val _endPhotoBytes = MutableStateFlow<ByteArray?>(null)
    val endPhotoBytes: StateFlow<ByteArray?> = _endPhotoBytes.asStateFlow()

    // Pending confirmation (in-memory only, shown in confirm screen)
    private val _startPendingBytes = MutableStateFlow<ByteArray?>(null)
    val startPendingBytes: StateFlow<ByteArray?> = _startPendingBytes.asStateFlow()

    private val _endPendingBytes = MutableStateFlow<ByteArray?>(null)
    val endPendingBytes: StateFlow<ByteArray?> = _endPendingBytes.asStateFlow()

    private val _isRestored = MutableStateFlow(false)
    val isRestored: StateFlow<Boolean> = _isRestored.asStateFlow()

    private val _uiState = MutableStateFlow<ExecutionUiState>(ExecutionUiState.Loading)
    val uiState: StateFlow<ExecutionUiState> = _uiState.asStateFlow()

    private var capturedStartTime: Instant? = null
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId).filterNotNull().first()
            val isRejected = task.status == TaskStatus.REJECTED

            // Reopen rejected task: resets status to IN_PROGRESS, clears end photo in DB
            if (isRejected) {
                taskRepository.reopenTask(taskId)
                photoStore.delete(taskId, "end")
            }

            if (task.status == TaskStatus.IN_PROGRESS || isRejected) {
                val startBytes = photoStore.load(taskId, "start")
                val endBytes   = if (isRejected) null else photoStore.load(taskId, "end")
                val savedObs   = draftStore.loadObservations(taskId)

                if (startBytes != null) _startPhotoBytes.value = startBytes
                if (endBytes   != null) _endPhotoBytes.value   = endBytes
                if (savedObs.isNotBlank()) _observations.value = savedObs

                val startInstant = task.startTime
                    ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                if (startInstant != null) {
                    capturedStartTime = startInstant
                    startTimerFrom(startInstant)
                }

                _step.value = when {
                    endBytes   != null    -> 4
                    savedObs.isNotBlank() -> 3
                    startBytes != null    -> 2
                    else                  -> 1
                }

                if (startBytes != null || endBytes != null || savedObs.isNotBlank()) {
                    _isRestored.value = true
                }
            }

            _uiState.value = ExecutionUiState.Idle
        }
    }

    // ── Start photo ───────────────────────────────────────────────────────────

    fun onStartPhotoTaken(rawBytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _startPendingBytes.value = rawBytes.withTimestamp()
        }
    }

    fun confirmStartPhoto() {
        val bytes = _startPendingBytes.value ?: return
        _startPendingBytes.value = null
        _startPhotoBytes.value = bytes
        photoStore.save(taskId, "start", bytes)

        if (capturedStartTime == null) {
            val now = Instant.now()
            capturedStartTime = now
            startTimerFrom(now)
            viewModelScope.launch { taskRepository.markTaskStarted(taskId, now.toString()) }
        }
        _step.value = 2
    }

    fun retakeStartPhoto() {
        _startPendingBytes.value = null
    }

    fun retakeStartPhotoFromTimer() {
        _startPhotoBytes.value = null
        photoStore.delete(taskId, "start")
        _step.value = 1
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun goToStep3() { _step.value = 3 }

    fun goToStep4() { _step.value = 4 }

    // ── Observations ──────────────────────────────────────────────────────────

    fun setObservations(text: String) {
        _observations.value = text
        viewModelScope.launch { draftStore.saveObservations(taskId, text) }
    }

    // ── End photo ─────────────────────────────────────────────────────────────

    fun onEndPhotoTaken(rawBytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _endPendingBytes.value = rawBytes.withTimestamp()
        }
    }

    fun confirmEndPhoto() {
        val bytes = _endPendingBytes.value ?: return
        _endPendingBytes.value = null
        _endPhotoBytes.value = bytes
        photoStore.save(taskId, "end", bytes)
    }

    fun retakeEndPhoto() {
        _endPendingBytes.value = null
        _endPhotoBytes.value = null
        photoStore.delete(taskId, "end")
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    fun clearError() {
        if (_uiState.value is ExecutionUiState.Error) _uiState.value = ExecutionUiState.Idle
    }

    fun submit() {
        val startBytes = _startPhotoBytes.value ?: return
        val endBytes   = _endPhotoBytes.value   ?: return
        val startTime  = capturedStartTime?.toString() ?: Instant.now().toString()

        viewModelScope.launch {
            _uiState.value = ExecutionUiState.Uploading

            val session = sessionManager.getSession().first()
            if (session == null) {
                _uiState.value = ExecutionUiState.Error("Sin sesión activa")
                return@launch
            }

            val startResult = storageRepository.uploadTaskPhoto(
                companyId  = session.companyId,
                taskId     = taskId,
                type       = "start",
                imageBytes = startBytes,
            )
            if (startResult.isFailure) {
                _uiState.value = ExecutionUiState.Error(
                    startResult.exceptionOrNull()?.message ?: "Error al subir foto de inicio"
                )
                return@launch
            }

            taskRepository.startTask(taskId, startResult.getOrThrow(), startTime)

            val endResult = storageRepository.uploadTaskPhoto(
                companyId  = session.companyId,
                taskId     = taskId,
                type       = "end",
                imageBytes = endBytes,
            )
            if (endResult.isFailure) {
                _uiState.value = ExecutionUiState.Error(
                    endResult.exceptionOrNull()?.message ?: "Error al subir foto de finalización"
                )
                return@launch
            }

            val submitResult = taskRepository.submitTask(
                taskId       = taskId,
                photoEndUrl  = endResult.getOrThrow(),
                observations = _observations.value.trim().takeIf { it.isNotBlank() },
            )

            if (submitResult.isSuccess) {
                draftStore.clearDraft(taskId)
                photoStore.clear(taskId)
            }

            _uiState.value = submitResult.fold(
                onSuccess = { ExecutionUiState.Success },
                onFailure = { ExecutionUiState.Error(it.message ?: "Error al enviar tarea") },
            )
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun startTimerFrom(startTime: Instant) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                _elapsedSeconds.value = Duration.between(startTime, Instant.now()).seconds.coerceAtLeast(0L)
                delay(1_000)
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private fun ByteArray.withTimestamp(): ByteArray = try {
        val bitmap = BitmapFactory.decodeByteArray(this, 0, size)
            ?.copy(Bitmap.Config.ARGB_8888, true) ?: return this
        val canvas = Canvas(bitmap)
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val paint = Paint().apply {
            color       = android.graphics.Color.WHITE
            textSize    = bitmap.height * 0.04f
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        }
        val textWidth = paint.measureText(timestamp)
        canvas.drawText(timestamp, bitmap.width - textWidth - bitmap.width * 0.02f, bitmap.height * 0.96f, paint)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.toByteArray()
    } catch (_: Exception) { this }
}
