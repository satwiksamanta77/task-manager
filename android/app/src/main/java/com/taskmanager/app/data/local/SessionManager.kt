package com.taskmanager.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the auth token in EncryptedSharedPreferences (AES256-SIV for keys,
 * AES256-GCM for values), so it is never stored in cleartext on the device.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "session_enc",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _tokenFlow = MutableStateFlow(prefs.getString(TOKEN_KEY, null))
    val tokenFlow: Flow<String?> = _tokenFlow.asStateFlow()

    suspend fun saveToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
        _tokenFlow.value = token
    }

    suspend fun clearToken() {
        prefs.edit().remove(TOKEN_KEY).apply()
        _tokenFlow.value = null
    }

    private companion object {
        const val TOKEN_KEY = "auth_token"
    }
}
