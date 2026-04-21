package com.cumplr.app.ui.chief

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

sealed class EditTaskUiState {
    object Idle        : EditTaskUiState()
    object Loading     : EditTaskUiState()
    object Submitting  : EditTaskUiState()
    object Success     : EditTaskUiState()
    data class Error(val message: String) : EditTaskUiState()
}

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    // ── Form fields ───────────────────────────────────────────────────────────
    var title          by mutableStateOf("")
    var description    by mutableStateOf("")
    var location       by mutableStateOf("")
    var selectedWorker by mutableStateOf<User?>(null)
    var deadlineMillis by mutableStateOf<Long?>(null)
    var deadlineHour   by mutableIntStateOf(9)
    var deadlineMinute by mutableIntStateOf(0)

    val deadlineIso: String?
        get() = deadlineMillis?.let { millis ->
            runCatching {
                val localDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                localDate.atTime(LocalTime.of(deadlineHour, deadlineMinute))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toString()
            }.getOrNull()
        }

    // ── State ─────────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<EditTaskUiState>(EditTaskUiState.Loading)
    val uiState: StateFlow<EditTaskUiState> = _uiState.asStateFlow()

    private val session = authRepository.getCurrentSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val workers: StateFlow<List<User>> = session.flatMapLatest { s ->
        if (s != null) userRepository.getWorkersByCompany(s.companyId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var assignedToId: String? = null

    val isFormValid: Boolean
        get() = title.isNotBlank() && selectedWorker != null

    init {
        // Load task and pre-fill form
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId).filterNotNull().first()
            title       = task.title
            description = task.description.orEmpty()
            location    = task.location.orEmpty()
            assignedToId = task.assignedTo

            task.deadline?.let { iso ->
                runCatching {
                    val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
                    // Store date as UTC epoch millis at midnight (matches DatePicker format)
                    deadlineMillis = zdt.toLocalDate()
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                    deadlineHour   = zdt.hour
                    deadlineMinute = zdt.minute
                }
            }

            _uiState.value = EditTaskUiState.Idle
        }

        // Resolve the assigned worker once the workers list loads
        viewModelScope.launch {
            workers.collect { list ->
                val id = assignedToId ?: return@collect
                if (selectedWorker == null && list.isNotEmpty()) {
                    selectedWorker = list.find { it.id == id }
                }
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun submit() {
        val worker = selectedWorker ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.value = EditTaskUiState.Submitting
            val result = taskRepository.updateTask(
                taskId      = taskId,
                title       = title.trim(),
                description = description.trim().takeIf { it.isNotBlank() },
                location    = location.trim().takeIf { it.isNotBlank() },
                deadline    = deadlineIso,
                assignedTo  = worker.id,
                priority    = TaskPriority.MEDIUM,
            )
            _uiState.value = if (result.isSuccess) EditTaskUiState.Success
                             else EditTaskUiState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar la tarea")
        }
    }

    fun dismissError() {
        if (_uiState.value is EditTaskUiState.Error) _uiState.value = EditTaskUiState.Idle
    }
}
