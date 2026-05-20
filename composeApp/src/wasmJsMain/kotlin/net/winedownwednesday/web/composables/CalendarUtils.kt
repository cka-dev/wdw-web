package net.winedownwednesday.web.composables

import net.winedownwednesday.web.utils.toEventLocalDate

/**
 * Utilities for "Add to Calendar" functionality.
 * Supports Google Calendar URLs and .ics file generation.
 *
 * All WDW events take place in Atlanta, GA.
 * Times are interpreted in America/New_York (Eastern Time).
 */
object CalendarUtils {

    /** IANA timezone for all WDW events. */
    private const val EVENT_TZ = "America/New_York"

    /**
     * Build a Google Calendar "add event" URL.
     * Opens in a new browser tab — no auth required to view the form.
     */
    fun buildGoogleCalendarUrl(
        title: String,
        date: String,
        time: String?,
        location: String,
        description: String
    ): String {
        val localDate = date.toEventLocalDate() ?: return ""
        val dateStr = padInt(localDate.year, 4) +
            padInt(localDate.monthNumber, 2) +
            padInt(localDate.dayOfMonth, 2)

        // If time is provided, create a timed event (2 hours default)
        // Otherwise, create an all-day event
        val dates = if (time != null) {
            val timeParsed = parseTime(time)
            if (timeParsed != null) {
                val (hour, minute) = timeParsed
                val startTime = padInt(hour, 2) +
                    padInt(minute, 2) + "00"
                val endHour = (hour + 2) % 24
                val endTime = padInt(endHour, 2) +
                    padInt(minute, 2) + "00"
                "${dateStr}T$startTime/${dateStr}T$endTime"
            } else {
                "$dateStr/$dateStr"
            }
        } else {
            "$dateStr/$dateStr"
        }

        val params = buildString {
            append("https://calendar.google.com/calendar/render")
            append("?action=TEMPLATE")
            append("&text=")
            append(urlEncode(title))
            append("&dates=")
            append(dates)
            append("&location=")
            append(urlEncode(location))
            append("&details=")
            append(urlEncode(description))
            append("&ctz=")
            append(EVENT_TZ)
        }
        return params
    }

    /**
     * Build an .ics (iCalendar) content string for download.
     * Compatible with Apple Calendar, Outlook, and other
     * calendar apps.
     */
    fun buildIcsContent(
        title: String,
        date: String,
        time: String?,
        location: String,
        description: String
    ): String {
        val localDate = date.toEventLocalDate() ?: return ""
        val dateStr = padInt(localDate.year, 4) +
            padInt(localDate.monthNumber, 2) +
            padInt(localDate.dayOfMonth, 2)

        val (dtStart, dtEnd) = if (time != null) {
            val timeParsed = parseTime(time)
            if (timeParsed != null) {
                val (hour, minute) = timeParsed
                val start = dateStr + "T" +
                    padInt(hour, 2) + padInt(minute, 2) + "00"
                val endHour = (hour + 2) % 24
                val end = dateStr + "T" +
                    padInt(endHour, 2) +
                    padInt(minute, 2) + "00"
                "DTSTART;TZID=$EVENT_TZ:$start" to
                    "DTEND;TZID=$EVENT_TZ:$end"
            } else {
                "DTSTART;VALUE=DATE:$dateStr" to
                    "DTEND;VALUE=DATE:$dateStr"
            }
        } else {
            "DTSTART;VALUE=DATE:$dateStr" to
                "DTEND;VALUE=DATE:$dateStr"
        }

        // Escape ICS special characters
        val escapedTitle = icsEscape(title)
        val escapedLocation = icsEscape(location)
        val escapedDesc = icsEscape(description)

        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Wine Down Wednesday//EN")
            // VTIMEZONE block for Apple Calendar / Outlook
            appendLine("BEGIN:VTIMEZONE")
            appendLine("TZID:$EVENT_TZ")
            appendLine("BEGIN:STANDARD")
            appendLine("DTSTART:19701101T020000")
            appendLine("RRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU")
            appendLine("TZOFFSETFROM:-0400")
            appendLine("TZOFFSETTO:-0500")
            appendLine("TZNAME:EST")
            appendLine("END:STANDARD")
            appendLine("BEGIN:DAYLIGHT")
            appendLine("DTSTART:19700308T020000")
            appendLine("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU")
            appendLine("TZOFFSETFROM:-0500")
            appendLine("TZOFFSETTO:-0400")
            appendLine("TZNAME:EDT")
            appendLine("END:DAYLIGHT")
            appendLine("END:VTIMEZONE")
            appendLine("BEGIN:VEVENT")
            appendLine(dtStart)
            appendLine(dtEnd)
            appendLine("SUMMARY:$escapedTitle")
            appendLine("LOCATION:$escapedLocation")
            appendLine("DESCRIPTION:$escapedDesc")
            appendLine("END:VEVENT")
            appendLine("END:VCALENDAR")
        }
    }

    /**
     * Build a data URI from .ics content for download.
     */
    fun buildIcsDataUri(icsContent: String): String {
        return "data:text/calendar;charset=utf-8," +
            urlEncode(icsContent)
    }

    /**
     * Parse time string like "8:00 PM", "20:00", "7:30 pm"
     * into (hour24, minute) pair.
     */
    private fun parseTime(time: String): Pair<Int, Int>? {
        val cleaned = time.trim().uppercase()

        // Try "H:MM AM/PM" format
        val amPmRegex = Regex(
            """(\d{1,2}):(\d{2})\s*(AM|PM)"""
        )
        amPmRegex.find(cleaned)?.let { match ->
            var hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            val amPm = match.groupValues[3]
            if (amPm == "PM" && hour != 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0
            return hour to minute
        }

        // Try "HH:MM" 24-hour format
        val h24Regex = Regex("""(\d{1,2}):(\d{2})""")
        h24Regex.find(cleaned)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            return hour to minute
        }

        return null
    }

    /** Pad an integer with leading zeros. */
    private fun padInt(value: Int, length: Int): String {
        val s = value.toString()
        return if (s.length < length) {
            "0".repeat(length - s.length) + s
        } else {
            s
        }
    }

    /** Simple URL-encoding for calendar URLs. */
    private fun urlEncode(s: String): String {
        return s.replace(" ", "%20")
            .replace("&", "%26")
            .replace("=", "%3D")
            .replace("#", "%23")
            .replace("\n", "%0A")
            .replace(",", "%2C")
    }

    /** Escape special characters for ICS format. */
    private fun icsEscape(s: String): String {
        return s.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }
}
