package net.winedownwednesday.web.utils

import kotlinx.datetime.LocalDate

/**
 * Parses an event date string that may be in one of two formats:
 *   - Legacy comma format:  "2025, 3, 19"  (old backend behavior)
 *   - ISO 8601 (standard):  "2025-03-19"   (new backend behavior)
 *
 * Returns null if the string is empty or cannot be parsed in either format.
 * All callers should handle null gracefully (skip event, show fallback text, etc.)
 */
fun String.toEventLocalDate(): LocalDate? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null

    return try {
        when {
            // ISO 8601 datetime: "2025-03-19T00:00:00" or "2025-03-19T00:00:00.000Z" — extract date part
            trimmed.contains("T") -> {
                val datePart = trimmed.substringBefore("T")
                if (datePart.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) LocalDate.parse(datePart) else null
            }

            // ISO 8601 date only: "2025-03-19"
            trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}""")) ->
                LocalDate.parse(trimmed)

            // Legacy comma format: "2025, 3, 19"
            trimmed.contains(",") -> {
                val parts = trimmed.split(",").map { it.trim().toInt() }
                if (parts.size == 3) LocalDate(parts[0], parts[1], parts[2]) else null
            }

            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Formats a LocalDate to a human-readable display string.
 * Example: LocalDate(2025, 3, 19) -> "Mar 19, 2025"
 */
fun LocalDate.toDisplayString(): String {
    val month = when (monthNumber) {
        1  -> "Jan";  2  -> "Feb";  3  -> "Mar";  4  -> "Apr"
        5  -> "May";  6  -> "Jun";  7  -> "Jul";  8  -> "Aug"
        9  -> "Sep";  10 -> "Oct";  11 -> "Nov";  else -> "Dec"
    }
    return "$month $dayOfMonth, $year"
}

/**
 * Convenience: parse and format in one step for display in UI.
 * Falls back to the raw string if the date cannot be parsed.
 */
fun String.toEventDisplayDate(): String =
    toEventLocalDate()?.toDisplayString() ?: this
