package com.cumplr.app.ui.chief

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.model.User
import com.cumplr.core.data.session.AuthEvent
import com.cumplr.core.data.session.AuthEventBus
import com.cumplr.core.domain.repository.AuthRepository
import com.cumplr.core.domain.repository.TaskRepository
import com.cumplr.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 30_000L

data class TaskWithWorker(val task: Task, val worker: User?)

@HiltViewModel
class ChiefHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val authEventBus: AuthEventBus,
) : ViewModel() {

    private val session = authRepository.getCurrentSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _didLogOut = MutableStateFlow(false)
    val didLogOut: StateFlow<Boolean> = _didLogOut.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val chiefName: StateFlow<String> = session.map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val tasks = session.flatMapLatest { s ->
        if (s != null) taskRepository.getTasksByCompany(s.companyId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workers: StateFlow<List<User>> = session.flatMapLatest { s ->
        if (s != null) userRepository.getWorkersByCompany(s.companyId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasksWithWorkers: StateFlow<List<TaskWithWorker>> = combine(tasks, workers) { taskList, userList ->
        taskList.map { task -> TaskWithWorker(task, userList.find { it.id == task.assignedTo }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTasksCount: StateFlow<Int> = session.flatMapLatest { s ->
        if (s != null) taskRepository.getActiveTasksCount(s.companyId) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val completionRate: StateFlow<Float> = session.flatMapLatest { s ->
        if (s != null) taskRepository.getCompletionRate(s.companyId) else flowOf(0f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val overdueCount: StateFlow<Int> = session.flatMapLatest { s ->
        if (s != null) taskRepository.getOverdueCount(s.companyId) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val activeWorkersCount: StateFlow<Int> = workers
        .map { list -> list.count { it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val pendingReviewCount: StateFlow<Int> = tasks
        .map { list -> list.count { it.status == TaskStatus.SUBMITTED || it.status == TaskStatus.UNDER_REVIEW } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var pollingJob: Job? = null

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authEventBus.events.collect { event ->
                if (event is AuthEvent.SessionExpired) _didLogOut.value = true
            }
        }
        viewModelScope.launch {
            val s = authRepository.getCurrentSession().first { it != null } ?: return@launch
            taskRepository.refreshCompanyTasks(s.companyId)
            userRepository.refreshCompanyUsers(s.companyId)
            taskRepository.startRealtimeForChief(s.companyId)
        }
        startPolling()
    }

    override fun onCleared() {
        taskRepository.stopRealtime()
        super.onCleared()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val s = session.value ?: continue
                taskRepository.refreshCompanyTasks(s.companyId)
                userRepository.refreshCompanyUsers(s.companyId)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val s = session.value ?: return@launch
            taskRepository.refreshCompanyTasks(s.companyId)
            userRepository.refreshCompanyUsers(s.companyId)
            _isRefreshing.value = false
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId).onFailure {
                _errorMessage.value = "No se pudo eliminar la tarea"
            }
        }
    }

    fun reassignTask(taskId: String, newAssignedTo: String) {
        viewModelScope.launch {
            taskRepository.reassignTask(taskId, newAssignedTo).onFailure {
                _errorMessage.value = "No se pudo reasignar la tarea"
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _didLogOut.value = true
        }
    }
}
