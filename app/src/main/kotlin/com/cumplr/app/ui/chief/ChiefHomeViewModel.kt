package com.cumplr.app.ui.chief

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.model.User
import com.cumplr.core.domain.repository.AuthRepository
import com.cumplr.core.domain.repository.TaskRepository
import com.cumplr.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskWithWorker(val task: Task, val worker: User?)

@HiltViewModel
class ChiefHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val session = authRepository.getCurrentSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _didLogOut = MutableStateFlow(false)
    val didLogOut: StateFlow<Boolean> = _didLogOut.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val tasks = session.flatMapLatest { s ->
        if (s != null) taskRepository.getTasksByCompany(s.companyId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val users = session.flatMapLatest { s ->
        if (s != null) userRepository.getWorkersByCompany(s.companyId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasksWithWorkers: StateFlow<List<TaskWithWorker>> = combine(tasks, users) { taskList, userList ->
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

    val activeWorkersCount: StateFlow<Int> = users
        .map { list -> list.count { it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            val s = authRepository.getCurrentSession().first { it != null } ?: return@launch
            taskRepository.refreshCompanyTasks(s.companyId)
            userRepository.refreshCompanyUsers(s.companyId)
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

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _didLogOut.value = true
        }
    }
}
