package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.SearchResponse
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager

class SearchRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun search(query: String): SearchResponse =
        unwrap(api.search(authHeader(), query.trim()))
}
