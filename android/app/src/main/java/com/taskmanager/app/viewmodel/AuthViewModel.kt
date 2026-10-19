package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.local.SessionManager
import com.taskmanager.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val repo: AuthRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            try {
                val res = repo.login(email.trim(), password)
                session.saveToken(res.token)
                _uiState.value = AuthUiState()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Login failed")
            }
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            try {
                val res = repo.register(email.trim(), password, name.trim())
                session.saveToken(res.token)
                _uiState.value = AuthUiState()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Registration failed")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
