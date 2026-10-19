package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.AuthResponse
import com.taskmanager.app.data.api.LoginRequest
import com.taskmanager.app.data.api.OkResponse
import com.taskmanager.app.data.api.RegisterRequest
import com.taskmanager.app.data.api.UserResponse
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager
import kotlinx.coroutines.flow.first

abstract class BaseRepository(private val session: SessionManager) {

    protected suspend fun authHeader(): String {
        val token = session.tokenFlow.first().orEmpty()
        return "Bearer $token"
    }
}

class AuthRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun register(email: String, password: String, name: String): AuthResponse =
        unwrap(api.register(RegisterRequest(email, password, name)))

    suspend fun login(email: String, password: String): AuthResponse =
        unwrap(api.login(LoginRequest(email, password)))

    suspend fun logout(): OkResponse =
        unwrap(api.logout(authHeader()))

    suspend fun me(): UserResponse =
        unwrap(api.me(authHeader()))
}
