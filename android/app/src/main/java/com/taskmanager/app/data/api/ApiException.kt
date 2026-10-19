package com.taskmanager.app.data.api

import retrofit2.Response

class ApiException(message: String) : Exception(message)

private val errorFieldRegex = Regex(""""error"\s*:\s*"([^"]*)"""")

/**
 * Returns the response body on success, otherwise throws ApiException with the
 * server's human readable message from the {"error": "..."} body.
 */
suspend fun <T> unwrap(response: Response<T>): T {
    if (response.isSuccessful) {
        return response.body() ?: throw ApiException("Empty response from server")
    }
    val raw = try {
        response.errorBody()?.string()
    } catch (e: Exception) {
        null
    }
    val message = raw?.let { errorFieldRegex.find(it)?.groupValues?.get(1) }
        ?: "Request failed (${response.code()})"
    throw ApiException(message)
}
