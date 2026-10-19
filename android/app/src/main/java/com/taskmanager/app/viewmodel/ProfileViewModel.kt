package com.taskmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.api.User
import com.taskmanager.app.data.local.SessionManager
import com.taskmanager.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

class ProfileViewModel(
    private val repo: AuthRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(loading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val res = repo.me()
                _uiState.value = ProfileUiState(user = res.user)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load profile"
                )
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.logout()
            } catch (e: Exception) {
                // Token may already be invalid; still clear local session.
            }
            session.clearToken()
            onDone()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
