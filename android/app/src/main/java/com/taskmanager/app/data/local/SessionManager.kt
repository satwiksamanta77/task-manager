package com.taskmanager.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore("session")

class SessionManager(private val context: Context) {

    private companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    val tokenFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun clearToken() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
        }
    }
}
