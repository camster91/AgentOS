package com.agentOS.agents

import com.agentOS.ai.GeminiClient
import com.agentOS.api.*
import java.text.SimpleDateFormat
import java.util.*

data class CalendarEvent(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String? = null
)

class CalendarAgent(private val storage: StorageAPI, private val gemini: GeminiClient) : Agent() {

    override val scope = AgentScope(
        id = "com.agentOS.calendar",
        name = "Calendar Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Natural language calendar. Schedule via conversation.",
        capabilities = setOf("storage", "notifications", "calendar", "ui"),
        optionalCapabilities = setOf("contacts", "timezone"),
        storageQuotaBytes = 100_000_000
    )

    override val api: AgentAPI = NoOpAgentAPI(storage)

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val text = message.text.trim()
        val response = when {
            text.startsWith("schedule ", ignoreCase = true) -> {
                parseAndSchedule(text.removePrefix("schedule ").trim())
            }
            text.equals("list events", ignoreCase = true) ||
            text.equals("what's on my calendar", ignoreCase = true) -> {
                listUpcomingEvents()
            }
            text.startsWith("list events ", ignoreCase = true) -> {
                val dateStr = text.removePrefix("list events ").trim()
                listEventsOnDate(dateStr)
            }
            text.startsWith("cancel event ", ignoreCase = true) -> {
                val query = text.removePrefix("cancel event ").trim()
                cancelEvent(query)
            }
            else -> aiChat(text)
        }
        return ChatMessage("assistant", response)
    }

    private suspend fun parseAndSchedule(input: String): String {
        // Expected: "<title> on <date> at <time>"
        val onIndex = input.lastIndexOf(" on ", ignoreCase = true)
        if (onIndex < 0) return "Format: schedule <title> on <date> at <time>"

        val title = input.substring(0, onIndex).trim()
        val dateTimePart = input.substring(onIndex + 4).trim()

        val atIndex = dateTimePart.lastIndexOf(" at ", ignoreCase = true)
        val datePart: String
        val timePart: String
        if (atIndex >= 0) {
            datePart = dateTimePart.substring(0, atIndex).trim()
            timePart = dateTimePart.substring(atIndex + 4).trim()
        } else {
            datePart = dateTimePart
            timePart = "09:00"
        }

        val date = parseDate(datePart) ?: return "Could not parse date: $datePart"
        val time = parseTime(timePart) ?: return "Could not parse time: $timePart"

        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, time.first)
        cal.set(Calendar.MINUTE, time.second)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val startTime = cal.timeInMillis
        val endTime = startTime + 3600_000 // default 1 hour

        val id = UUID.randomUUID().toString().take(8)
        val event = CalendarEvent(id, title, startTime, endTime)
        storage.writeString("event-$id", serializeEvent(event))

        val df = SimpleDateFormat("EEE MMM d, yyyy 'at' h:mm a", Locale.US)
        return "Scheduled \"$title\" for ${df.format(Date(startTime))} [ID: $id]"
    }

    private fun parseDate(input: String): Date? {
        val lower = input.lowercase(Locale.US).trim()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (lower) {
            "today" -> return cal.time
            "tomorrow" -> { cal.add(Calendar.DAY_OF_MONTH, 1); return cal.time }
        }

        // Day of week: monday, tuesday, etc.
        val dayOfWeek = mapOf(
            "sunday" to Calendar.SUNDAY, "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY, "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY, "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY
        )
        dayOfWeek[lower]?.let { target ->
            val current = cal.get(Calendar.DAY_OF_WEEK)
            var diff = target - current
            if (diff <= 0) diff += 7
            cal.add(Calendar.DAY_OF_MONTH, diff)
            return cal.time
        }

        // Try common date formats
        val formats = listOf(
            "MMM d", "MMM d, yyyy", "MMMM d", "MMMM d, yyyy",
            "yyyy-MM-dd", "MM/dd/yyyy", "MM/dd", "d MMM", "d MMM yyyy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.isLenient = false
                val parsed = sdf.parse(input) ?: continue
                val parsedCal = Calendar.getInstance()
                parsedCal.time = parsed
                // If no year was in format, use current year
                if (!fmt.contains("yyyy")) {
                    parsedCal.set(Calendar.YEAR, cal.get(Calendar.YEAR))
                }
                parsedCal.set(Calendar.HOUR_OF_DAY, 0)
                parsedCal.set(Calendar.MINUTE, 0)
                parsedCal.set(Calendar.SECOND, 0)
                parsedCal.set(Calendar.MILLISECOND, 0)
                return parsedCal.time
            } catch (_: Exception) { /* try next */ }
        }

        return null
    }

    private fun parseTime(input: String): Pair<Int, Int>? {
        val lower = input.lowercase(Locale.US).trim()

        // "3pm", "3 pm", "3:30pm", "3:30 pm"
        val ampmRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)""")
        ampmRegex.find(lower)?.let { match ->
            var hour = match.groupValues[1].toInt()
            val min = match.groupValues[2].toIntOrNull() ?: 0
            val ampm = match.groupValues[3]
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return Pair(hour, min)
        }

        // "15:30" or "9:00"
        val milRegex = Regex("""(\d{1,2}):(\d{2})""")
        milRegex.find(lower)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val min = match.groupValues[2].toInt()
            if (hour in 0..23 && min in 0..59) return Pair(hour, min)
        }

        return null
    }

    private suspend fun listUpcomingEvents(): String {
        val now = System.currentTimeMillis()
        val weekLater = now + 7 * 24 * 3600_000L
        val events = getAllEvents().filter { it.startTime in now..weekLater }
            .sortedBy { it.startTime }
        if (events.isEmpty()) return "No upcoming events in the next 7 days."
        val df = SimpleDateFormat("EEE MMM d 'at' h:mm a", Locale.US)
        return events.joinToString("\n") { "[${it.id}] ${it.title} — ${df.format(Date(it.startTime))}" }
    }

    private suspend fun listEventsOnDate(dateStr: String): String {
        val date = parseDate(dateStr) ?: return "Could not parse date: $dateStr"
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 24 * 3600_000L

        val events = getAllEvents().filter { it.startTime in dayStart until dayEnd }
            .sortedBy { it.startTime }
        if (events.isEmpty()) return "No events on ${SimpleDateFormat("EEE MMM d, yyyy", Locale.US).format(date)}."
        val df = SimpleDateFormat("h:mm a", Locale.US)
        return events.joinToString("\n") { "[${it.id}] ${it.title} at ${df.format(Date(it.startTime))}" }
    }

    private suspend fun cancelEvent(query: String): String {
        // Try by ID
        val byId = storage.readString("event-$query")
        if (byId != null) {
            storage.delete("event-$query")
            val event = deserializeEvent(byId)
            return if (event != null) "Cancelled \"${event.title}\" [${event.id}]"
            else "Event $query cancelled."
        }

        // Try by title
        val events = getAllEvents()
        val match = events.find { it.title.equals(query, ignoreCase = true) }
        if (match != null) {
            storage.delete("event-${match.id}")
            return "Cancelled \"${match.title}\" [${match.id}]"
        }

        return "Event not found: $query"
    }

    private suspend fun getAllEvents(): List<CalendarEvent> {
        return storage.listKeys()
            .filter { it.startsWith("event-") }
            .mapNotNull { key ->
                storage.readString(key)?.let { deserializeEvent(it) }
            }
    }

    private fun serializeEvent(event: CalendarEvent): String {
        // Format: id|title|startTime|endTime|location
        return "${event.id}|${event.title}|${event.startTime}|${event.endTime}|${event.location ?: ""}"
    }

    private fun deserializeEvent(data: String): CalendarEvent? {
        val parts = data.split("|", limit = 5)
        if (parts.size < 5) return null
        return CalendarEvent(
            id = parts[0],
            title = parts[1],
            startTime = parts[2].toLongOrNull() ?: return null,
            endTime = parts[3].toLongOrNull() ?: return null,
            location = parts[4].ifBlank { null }
        )
    }

    private fun aiChat(userMessage: String): String {
        return gemini.chat(
            systemPrompt = """You are the Calendar Agent for AgentOS. You help users manage calendar events.
Available commands you can suggest: schedule <title> on <date> at <time>, list events, list events <date>, what's on my calendar, cancel event <id or title>.
Dates: today, tomorrow, Monday, Jan 15, 2026-01-15. Times: 3pm, 3:30pm, 15:30.
Be helpful and concise. If the user's intent maps to a command, tell them the exact command to use.""",
            userMessage = userMessage
        )
    }

    private fun helpText(): String {
        return """I don't understand that command. Available commands:
  schedule <title> on <date> at <time>
  list events
  list events <date>
  what's on my calendar
  cancel event <id or title>

Dates: today, tomorrow, Monday, Jan 15, 2026-01-15
Times: 3pm, 3:30pm, 15:30"""
    }
}
