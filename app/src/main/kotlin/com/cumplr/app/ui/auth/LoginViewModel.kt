package com.cumplr.app.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumplr.core.domain.enums.UserRole
import com.cumplr.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CumplrLogin"

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val role: UserRole) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        Log.d(TAG, "signIn() triggered — email='$email' passwordLen=${password.length}")
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.signIn(email.trim(), password).fold(
                onSuccess = { user ->
                    Log.d(TAG, "Login success — role=${user.role}")
                    _uiState.value = LoginUiState.Success(user.role)
                },
                onFailure = { e ->
                    Log.e(TAG, "Login failure — ${e.message}")
                    _uiState.value = LoginUiState.Error(e.message ?: "Error al iniciar sesión.")
                },
            )
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}
