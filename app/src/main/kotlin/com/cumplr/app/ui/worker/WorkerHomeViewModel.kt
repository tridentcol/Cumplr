package com.cumplr.app.ui.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.model.AppNotification
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.repository.AuthRepository
import com.cumplr.core.domain.repository.NotificationRepository
import com.cumplr.core.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 30_000L

@HiltViewModel
class WorkerHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _didLogOut    = MutableStateFlow(false)
    val didLogOut: StateFlow<Boolean> = _didLogOut.asStateFlow()

    val userName: StateFlow<String> = authRepository.getCurrentSession()
        .map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _isLoading    = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val tasks: StateFlow<List<Task>> = authRepository.getCurrentSession()
        .flatMapLatest { session ->
            if (session != null) taskRepository.getMyTasks(session.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = authRepository.getCurrentSession()
        .flatMapLatest { session ->
            if (session != null) notificationRepository.getNotifications(session.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = authRepository.getCurrentSession()
        .flatMapLatest { session ->
            if (session != null) notificationRepository.getUnreadCount(session.userId)
            else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession().first()
            if (session != null) taskRepository.refresh(session.userId)
            _isLoading.value = false
        }
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val session = authRepository.getCurrentSession().first() ?: continue
                taskRepository.refresh(session.userId)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val session = authRepository.getCurrentSession().first()
            if (session != null) taskRepository.refresh(session.userId)
            _isRefreshing.value = false
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { notificationRepository.markAsRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession().first() ?: return@launch
            notificationRepository.markAllRead(session.userId)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _didLogOut.value = true
        }
    }
}
