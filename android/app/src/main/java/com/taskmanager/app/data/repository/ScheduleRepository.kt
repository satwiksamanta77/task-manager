package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.ScheduleItem
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager

class ScheduleRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun getUpcoming(days: Int = 7): List<ScheduleItem> =
        unwrap(api.getSchedule(authHeader(), days)).items
}
