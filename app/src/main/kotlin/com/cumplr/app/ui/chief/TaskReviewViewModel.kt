package com.cumplr.app.ui.chief

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.model.User
import com.cumplr.core.domain.repository.TaskRepository
import com.cumplr.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReviewUiState {
    object Idle       : ReviewUiState()
    object Submitting : ReviewUiState()
    object Success    : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
}

@HiltViewModel
class TaskReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    val task: StateFlow<Task?> = taskRepository.getTask(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val worker: StateFlow<User?> = task.flatMapLatest { t ->
        if (t != null) userRepository.getUser(t.assignedTo) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Idle)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun approve(feedback: String) {
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Submitting
            val result = taskRepository.approveTask(taskId, feedback.trim().takeIf { it.isNotBlank() })
            _uiState.value = result.fold(
                onSuccess = { ReviewUiState.Success },
                onFailure = { ReviewUiState.Error(it.message ?: "Error al aprobar") },
            )
        }
    }

    fun reject(reason: String) {
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Submitting
            val result = taskRepository.rejectTask(taskId, reason.trim())
            _uiState.value = result.fold(
                onSuccess = { ReviewUiState.Success },
                onFailure = { ReviewUiState.Error(it.message ?: "Error al rechazar") },
            )
        }
    }
}
