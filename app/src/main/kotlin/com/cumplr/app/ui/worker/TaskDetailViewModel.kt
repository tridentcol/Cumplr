package com.cumplr.app.ui.worker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.model.Note
import com.cumplr.core.domain.model.Task
import com.cumplr.core.domain.repository.NoteRepository
import com.cumplr.core.domain.repository.TaskRepository
import com.cumplr.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    taskRepository: TaskRepository,
    userRepository: UserRepository,
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    val task: StateFlow<Task?> = taskRepository.getTask(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val assignerName: StateFlow<String?> = task
        .flatMapLatest { t ->
            if (t != null) userRepository.getUser(t.assignedBy).map { it?.name }
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notes: StateFlow<List<Note>> = noteRepository.getNotes(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isAddingNote = MutableStateFlow(false)
    val isAddingNote: StateFlow<Boolean> = _isAddingNote.asStateFlow()

    fun addNote(text: String) {
        viewModelScope.launch {
            _isAddingNote.value = true
            noteRepository.addNote(taskId, text)
            _isAddingNote.value = false
        }
    }
}
