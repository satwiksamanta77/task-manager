package com.taskmanager.app.ui.util

/**
 * Shows the date part of an ISO-8601 UTC timestamp, e.g. "2026-10-21".
 */
fun formatDueDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return iso.take(10)
}

fun capitalizeFirst(value: String): String =
    value.replaceFirstChar { it.uppercase() }
