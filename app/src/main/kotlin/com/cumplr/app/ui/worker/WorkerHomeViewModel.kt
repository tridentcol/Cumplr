package com.cumplr.app.ui.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkerHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _didLogOut = MutableStateFlow(false)
    val didLogOut: StateFlow<Boolean> = _didLogOut.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _didLogOut.value = true
        }
    }
}
