package com.agentOS.agents

import com.agentOS.ai.GeminiClient
import com.agentOS.api.Agent
import com.agentOS.api.AgentAPI
import com.agentOS.api.AgentScope
import com.agentOS.api.ChatMessage
import com.agentOS.api.StorageAPI
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class Task(
    val id: String,
    val title: String,
    val priority: Priority,
    val dueDate: Long?,
    val completed: Boolean,
    val createdAt: Long
)

enum class Priority { LOW, MEDIUM, HIGH }

class TasksAgent(private val storage: StorageAPI, private val gemini: GeminiClient) : Agent() {

    override val scope = AgentScope(
        id = "com.agentOS.tasks",
        name = "Tasks Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Task management with priorities, due dates, and filtering.",
        capabilities = setOf("storage", "ui")
    )

    override val api: AgentAPI = NoOpAgentAPI(storage)

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val text = message.text.trim()
        val lower = text.lowercase()

        val response = when {
            lower.startsWith("add task:") -> {
                addTask(text.substring(9).trim())
            }
            lower.startsWith("add task ") -> {
                addTask(text.substring(9).trim())
            }
            lower == "list tasks" -> listTasks(includeCompleted = false)
            lower == "all tasks" -> listTasks(includeCompleted = true)
            lower.startsWith("complete task ") -> {
                completeTask(text.substring(14).trim())
            }
            lower.startsWith("delete task ") -> {
                deleteTask(text.substring(12).trim())
            }
            lower == "due today" || lower == "tasks due today" -> dueToday()
            lower == "due this week" || lower == "tasks due this week" -> dueThisWeek()
            else -> aiChat(text)
        }
        return ChatMessage("assistant", response)
    }

    private suspend fun addTask(input: String): String {
        var title = input
        var priority = Priority.MEDIUM
        var dueDate: Long? = null

        // Extract priority
        val priorityRegex = Regex("""(?i)\bpriority:\s*(high|medium|low)\b""")
        val priorityMatch = priorityRegex.find(title)
        if (priorityMatch != null) {
            priority = Priority.valueOf(priorityMatch.groupValues[1].uppercase())
            title = title.removeRange(priorityMatch.range).trim()
        }

        // Extract due date
        val dueRegex = Regex("""(?i)\bdue:\s*(.+?)(?:\s+priority:|$)""")
        val dueMatch = dueRegex.find(title)
        if (dueMatch != null) {
            val dateStr = dueMatch.groupValues[1].trim()
            val parsedDate = parseDate(dateStr)
            if (parsedDate != null) {
                dueDate = parsedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            title = title.removeRange(dueMatch.range).trim()
        }

        val id = UUID.randomUUID().toString().take(8)
        val task = Task(
            id = id,
            title = title,
            priority = priority,
            dueDate = dueDate,
            completed = false,
            createdAt = System.currentTimeMillis()
        )

        storage.writeString("task_$id", serializeTask(task))

        val duePart = if (dueDate != null) {
            val date = java.time.Instant.ofEpochMilli(dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
            ", due ${date}"
        } else ""

        return "Task created: [${task.id}] ${task.title} (${task.priority}$duePart)"
    }

    private suspend fun listTasks(includeCompleted: Boolean): String {
        val tasks = getAllTasks()
        val filtered = if (includeCompleted) tasks else tasks.filter { !it.completed }

        if (filtered.isEmpty()) {
            return if (includeCompleted) "No tasks found." else "No incomplete tasks."
        }

        val sorted = filtered.sortedWith(
            compareBy<Task> { it.completed }
                .thenByDescending { it.priority.ordinal }
                .thenBy { it.dueDate ?: Long.MAX_VALUE }
        )

        return buildString {
            append(if (includeCompleted) "All tasks:" else "Tasks:")
            for (task in sorted) {
                val check = if (task.completed) "[x]" else "[ ]"
                val duePart = if (task.dueDate != null) {
                    val date = java.time.Instant.ofEpochMilli(task.dueDate)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    " due:$date"
                } else ""
                append("\n  $check [${task.id}] ${task.title} (${task.priority}$duePart)")
            }
        }
    }

    private suspend fun completeTask(query: String): String {
        val task = findTask(query) ?: return "Task not found: $query"
        val updated = task.copy(completed = true)
        storage.writeString("task_${task.id}", serializeTask(updated))
        return "Completed: ${task.title}"
    }

    private suspend fun deleteTask(query: String): String {
        val task = findTask(query) ?: return "Task not found: $query"
        storage.delete("task_${task.id}")
        return "Deleted: ${task.title}"
    }

    private suspend fun dueToday(): String {
        val today = LocalDate.now()
        val tasks = getAllTasks().filter { task ->
            !task.completed && task.dueDate != null &&
                    java.time.Instant.ofEpochMilli(task.dueDate)
                        .atZone(ZoneId.systemDefault()).toLocalDate() == today
        }

        if (tasks.isEmpty()) return "No tasks due today."

        return buildString {
            append("Tasks due today:")
            for (task in tasks) {
                append("\n  [${task.id}] ${task.title} (${task.priority})")
            }
        }
    }

    private suspend fun dueThisWeek(): String {
        val today = LocalDate.now()
        val weekEnd = today.plusDays(7)
        val tasks = getAllTasks().filter { task ->
            if (task.completed || task.dueDate == null) return@filter false
            val date = java.time.Instant.ofEpochMilli(task.dueDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(today) && !date.isAfter(weekEnd)
        }

        if (tasks.isEmpty()) return "No tasks due this week."

        return buildString {
            append("Tasks due this week:")
            for (task in tasks.sortedBy { it.dueDate }) {
                val date = java.time.Instant.ofEpochMilli(task.dueDate!!)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                append("\n  [${task.id}] ${task.title} (${task.priority}, due:$date)")
            }
        }
    }

    private suspend fun findTask(query: String): Task? {
        val tasks = getAllTasks()
        // Try matching by ID first
        val byId = tasks.find { it.id == query }
        if (byId != null) return byId
        // Try matching by title (case-insensitive)
        return tasks.find { it.title.equals(query, ignoreCase = true) }
    }

    private suspend fun getAllTasks(): List<Task> {
        return storage.listKeys()
            .filter { it.startsWith("task_") }
            .mapNotNull { key ->
                val raw = storage.readString(key)
                raw?.let { deserializeTask(it) }
            }
    }

    private fun parseDate(input: String): LocalDate? {
        val lower = input.lowercase().trim()
        val today = LocalDate.now()

        return when (lower) {
            "today" -> today
            "tomorrow" -> today.plusDays(1)
            "monday", "next monday" -> today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            "tuesday", "next tuesday" -> today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
            "wednesday", "next wednesday" -> today.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            "thursday", "next thursday" -> today.with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
            "friday", "next friday" -> today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
            "saturday", "next saturday" -> today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            "sunday", "next sunday" -> today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
            else -> {
                // Try ISO format (YYYY-MM-DD)
                try {
                    LocalDate.parse(lower, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: DateTimeParseException) {
                    // Try "Jan 15" style
                    try {
                        val formatter = DateTimeFormatter.ofPattern("MMM d")
                        val parsed = formatter.parse(input.trim())
                        var date = LocalDate.of(today.year,
                            parsed.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                            parsed.get(java.time.temporal.ChronoField.DAY_OF_MONTH))
                        if (date.isBefore(today)) date = date.plusYears(1)
                        date
                    } catch (e2: Exception) {
                        null
                    }
                }
            }
        }
    }

    private fun serializeTask(task: Task): String {
        return "${task.id}|${task.title}|${task.priority}|${task.dueDate ?: ""}|${task.completed}|${task.createdAt}"
    }

    private fun deserializeTask(raw: String): Task? {
        val parts = raw.split("|")
        if (parts.size < 6) return null
        return try {
            Task(
                id = parts[0],
                title = parts[1],
                priority = Priority.valueOf(parts[2]),
                dueDate = parts[3].toLongOrNull(),
                completed = parts[4].toBoolean(),
                createdAt = parts[5].toLong()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun aiChat(userMessage: String): String {
        return gemini.chat(
            systemPrompt = """You are the Tasks Agent for AgentOS. You help users manage tasks with priorities and due dates.
Available commands you can suggest: add task: <title> [priority: high/medium/low] [due: <date>], list tasks, all tasks, complete task <id|title>, delete task <id|title>, due today, due this week.
Date formats: today, tomorrow, next Monday, Jan 15, 2026-03-25.
Be helpful and concise. If the user's intent maps to a command, tell them the exact command to use.""",
            userMessage = userMessage
        )
    }

    private fun helpText(): String = """Tasks Agent — Commands:
  add task: <title> [priority: high/medium/low] [due: <date>]
  add task <title>          — Quick add with MEDIUM priority
  list tasks                — Show incomplete tasks
  all tasks                 — Show all tasks including completed
  complete task <id|title>  — Mark task as complete
  delete task <id|title>    — Remove a task
  due today                 — Tasks due today
  due this week             — Tasks due in next 7 days

  Date formats: today, tomorrow, next Monday, Jan 15, 2026-03-25"""
}
