package com.taskmanager.app.ui.util

/**
 * Shows the date part of an ISO-8601 UTC timestamp, e.g. "2026-10-21".
 */
fun formatDueDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return iso.take(10)
}

/** True when the ISO date is before today (UTC date part comparison). */
fun isOverdueDate(iso: String?): Boolean {
    if (iso.isNullOrBlank()) return false
    val date = iso.take(10)
    if (date.length != 10) return false
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date())
    return date < today
}

private val DAY_NAMES = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** "2026-10-21" -> "Wed, Oct 21". Falls back to the raw string. */
fun formatDayHeader(yyyyMmDd: String): String {
    return try {
        val parts = yyyyMmDd.split("-")
        val y = parts[0].toInt(); val m = parts[1].toInt(); val d = parts[2].toInt()
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(y, m - 1, d, 0, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        "${DAY_NAMES[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]}, ${MONTH_NAMES[m - 1]} $d"
    } catch (e: Exception) {
        yyyyMmDd
    }
}

fun capitalizeFirst(value: String): String =
    value.replaceFirstChar { it.uppercase() }
