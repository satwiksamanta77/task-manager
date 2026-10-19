package com.taskmanager.app.data.repository

import com.taskmanager.app.data.api.ApiService
import com.taskmanager.app.data.api.CreateLabelRequest
import com.taskmanager.app.data.api.LabelDto
import com.taskmanager.app.data.api.UpdateLabelRequest
import com.taskmanager.app.data.api.unwrap
import com.taskmanager.app.data.local.SessionManager

class LabelRepository(
    private val api: ApiService,
    session: SessionManager
) : BaseRepository(session) {

    suspend fun getLabels(): List<LabelDto> =
        unwrap(api.getLabels(authHeader())).labels

    suspend fun createLabel(name: String, color: String?): LabelDto =
        unwrap(api.createLabel(authHeader(), CreateLabelRequest(name.trim(), color))).label

    suspend fun updateLabel(id: String, name: String?, color: String?): LabelDto =
        unwrap(api.updateLabel(authHeader(), id, UpdateLabelRequest(name?.trim(), color))).label

    suspend fun deleteLabel(id: String) {
        unwrap(api.deleteLabel(authHeader(), id))
    }
}
