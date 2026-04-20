package com.cumplr.app.ui.worker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.data.session.SessionManager
import com.cumplr.core.domain.repository.StorageRepository
import com.cumplr.core.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class ExecutionUiState {
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
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _step           = MutableStateFlow(1)
    val step: StateFlow<Int>    = _step.asStateFlow()

    private val _elapsedSeconds       = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _observations           = MutableStateFlow("")
    val observations: StateFlow<String> = _observations.asStateFlow()

    private val _startPhotoBytes = MutableStateFlow<ByteArray?>(null)
    val startPhotoBytes: StateFlow<ByteArray?> = _startPhotoBytes.asStateFlow()

    private val _endPhotoBytes = MutableStateFlow<ByteArray?>(null)
    val endPhotoBytes: StateFlow<ByteArray?> = _endPhotoBytes.asStateFlow()

    private val _uiState              = MutableStateFlow<ExecutionUiState>(ExecutionUiState.Idle)
    val uiState: StateFlow<ExecutionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun onStartPhotoTaken(rawBytes: ByteArray) {
        _startPhotoBytes.value = rawBytes.withTimestamp()
        _step.value = 2
        startTimer()
    }

    fun goToStep3() { _step.value = 3 }

    fun goToStep4() { _step.value = 4 }

    fun setObservations(text: String) { _observations.value = text }

    fun onEndPhotoTaken(rawBytes: ByteArray) {
        _endPhotoBytes.value = rawBytes.withTimestamp()
    }

    fun clearError() {
        if (_uiState.value is ExecutionUiState.Error) _uiState.value = ExecutionUiState.Idle
    }

    fun submit() {
        val startBytes = _startPhotoBytes.value ?: return
        val endBytes   = _endPhotoBytes.value   ?: return
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

            taskRepository.startTask(taskId, startResult.getOrThrow())

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
            _uiState.value = submitResult.fold(
                onSuccess = { ExecutionUiState.Success },
                onFailure = { ExecutionUiState.Error(it.message ?: "Error al enviar tarea") },
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _elapsedSeconds.update { it + 1 }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    private fun ByteArray.withTimestamp(): ByteArray = try {
        val bitmap = BitmapFactory.decodeByteArray(this, 0, size)
            ?.copy(Bitmap.Config.ARGB_8888, true) ?: return this
        val canvas = Canvas(bitmap)
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val paint = Paint().apply {
            color     = android.graphics.Color.WHITE
            textSize  = bitmap.height * 0.04f
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
