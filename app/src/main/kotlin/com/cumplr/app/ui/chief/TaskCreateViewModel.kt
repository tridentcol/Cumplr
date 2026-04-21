package com.cumplr.app.ui.chief

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.model.User
import com.cumplr.core.domain.repository.AuthRepository
import com.cumplr.core.domain.repository.TaskRepository
import com.cumplr.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

sealed class CreateTaskUiState {
    object Idle        : CreateTaskUiState()
    object Submitting  : CreateTaskUiState()
    object Success     : CreateTaskUiState()
    data class Error(val message: String) : CreateTaskUiState()
}

@HiltViewModel
class TaskCreateViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    // ── Form fields ───────────────────────────────────────────────────────────
    var title       by mutableStateOf("")
    var description by mutableStateOf("")
    var selectedWorker by mutableStateOf<User?>(null)
    var deadlineMillis by mutableStateOf<Long?>(null)

    // ── State ─────────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<CreateTaskUiState>(CreateTaskUiState.Idle)
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    private val session = authRepository.getCurrentSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val workers: StateFlow<List<User>> = session.flatMapLatest { s ->
        if (s != null) userRepository.getWorkersByCompany(s.companyId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isFormValid: Boolean
        get() = title.isNotBlank() && selectedWorker != null

    // ── Actions ───────────────────────────────────────────────────────────────

    fun submit() {
        val worker = selectedWorker ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.value = CreateTaskUiState.Submitting
            val s = authRepository.getCurrentSession().first() ?: run {
                _uiState.value = CreateTaskUiState.Error("Sin sesión activa")
                return@launch
            }
            val deadlineStr = deadlineMillis?.let { Instant.ofEpochMilli(it).toString() }
            val result = taskRepository.createTask(
                title       = title.trim(),
                description = description.trim().takeIf { it.isNotBlank() },
                assignedTo  = worker.id,
                deadline    = deadlineStr,
                priority    = TaskPriority.MEDIUM,
                companyId   = s.companyId,
                assignedBy  = s.userId,
            )
            _uiState.value = if (result.isSuccess) {
                CreateTaskUiState.Success
            } else {
                CreateTaskUiState.Error(result.exceptionOrNull()?.message ?: "Error al crear la tarea")
            }
        }
    }

    fun dismissError() {
        _uiState.value = CreateTaskUiState.Idle
    }
}
