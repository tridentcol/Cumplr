package com.cumplr.app.ui.chief

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.enums.TaskStatus
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    authRepository: AuthRepository,
    taskRepository: TaskRepository,
    userRepository: UserRepository,
) : ViewModel() {

    private val session = authRepository.getCurrentSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val allUsers = session.flatMapLatest { s ->
        if (s != null) userRepository.getWorkersByCompany(s.companyId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allTasks: StateFlow<List<Task>> = session.flatMapLatest { s ->
        if (s != null) taskRepository.getTasksByCompany(s.companyId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 0 = Cargo, 1 = Nombre, 2 = Ambos */
    private val _searchMode = MutableStateFlow(2)
    val searchMode: StateFlow<Int> = _searchMode.asStateFlow()

    val filteredUsers: StateFlow<List<User>> =
        combine(allUsers, _searchQuery, _searchMode) { list, query, mode ->
            if (query.isBlank()) list
            else list.filter { user ->
                val byPosition = user.position?.contains(query, ignoreCase = true) == true
                val byName     = user.name.contains(query, ignoreCase = true)
                when (mode) {
                    0    -> byPosition
                    1    -> byName
                    else -> byPosition || byName
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun completionRateFor(userId: String): Float {
        val userTasks = allTasks.value.filter {
            it.assignedTo == userId && it.status != TaskStatus.ASSIGNED
        }
        if (userTasks.isEmpty()) return 0f
        return userTasks.count { it.status == TaskStatus.APPROVED }.toFloat() / userTasks.size
    }

    fun taskCountFor(userId: String): Int = allTasks.value.count { it.assignedTo == userId }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSearchMode(mode: Int)  { _searchMode.value  = mode }
}
