package com.agentOS.core

import com.agentOS.agents.CalendarAgent
import com.agentOS.agents.NotesAgent
import com.agentOS.agents.TasksAgent
import com.agentOS.agents.WeatherAgent
import com.agentOS.ai.GeminiClient
import com.agentOS.api.ChatMessage
import com.agentOS.api.AgentScope
import com.agentOS.core.marketplace.LocalMarketplace
import com.agentOS.core.storage.InMemoryStorage
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
    AgentOS.initialize()

    val notesScope = AgentScope(
        id = "com.agentOS.notes",
        name = "Notes Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Create, search, and link notes via conversation.",
        capabilities = setOf("storage", "ui")
    )
    val calendarScope = AgentScope(
        id = "com.agentOS.calendar",
        name = "Calendar Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Natural language calendar. Schedule via conversation.",
        capabilities = setOf("storage", "notifications", "calendar", "ui"),
        optionalCapabilities = setOf("contacts", "timezone"),
        storageQuotaBytes = 100_000_000
    )
    val weatherScope = AgentScope(
        id = "com.agentOS.weather",
        name = "Weather Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Real-time weather via Open-Meteo API. No API key needed.",
        capabilities = setOf("storage", "ui")
    )
    val tasksScope = AgentScope(
        id = "com.agentOS.tasks",
        name = "Tasks Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Task management with priorities, due dates, and filtering.",
        capabilities = setOf("storage", "ui")
    )

    val gemini = GeminiClient(
        apiKey = System.getenv("GEMINI_API_KEY") ?: "AIzaSyAPNvfGU6hlUAop3vE9BbJbukXKpvY6SB4"
    )

    val notesAgent = NotesAgent(InMemoryStorage(notesScope), gemini)
    val calendarAgent = CalendarAgent(InMemoryStorage(calendarScope), gemini)
    val weatherAgent = WeatherAgent(InMemoryStorage(weatherScope), gemini)
    val tasksAgent = TasksAgent(InMemoryStorage(tasksScope), gemini)

    AgentOS.registry.register(notesAgent)
    AgentOS.registry.register(calendarAgent)
    AgentOS.registry.register(weatherAgent)
    AgentOS.registry.register(tasksAgent)

    val marketplaceDir = File(System.getProperty("user.home"), ".agentos/marketplace")
    val sandboxRoot = File(System.getProperty("user.home"), ".agentos/sandbox")
    val marketplace = LocalMarketplace(marketplaceDir, sandboxRoot, AgentOS.registry)

    println()
    println("╔══════════════════════════════════════╗")
    println("║          AgentOS v0.1.0-alpha        ║")
    println("║   Your conversational agent OS       ║")
    println("╚══════════════════════════════════════╝")
    println()
    println("Agents loaded: ${AgentOS.registry.listAgents().joinToString(", ") { it.name }}")
    println("Type 'help' for commands, 'quit' to exit.")
    println()

    while (true) {
        print("agentos> ")
        val line = readlnOrNull()?.trim() ?: break
        if (line.isBlank()) continue

        when {
            line.equals("quit", ignoreCase = true) || line.equals("exit", ignoreCase = true) -> {
                println("Goodbye!")
                break
            }
            line.equals("agents", ignoreCase = true) -> {
                val agents = AgentOS.registry.listAgents()
                println("Registered agents (${agents.size}):")
                agents.forEach { println("  [${it.id}] ${it.name} v${it.version} — ${it.description}") }
            }
            line.equals("marketplace", ignoreCase = true) || line.equals("available", ignoreCase = true) -> {
                val available = marketplace.listAvailable()
                if (available.isEmpty()) {
                    println("No agents available in marketplace.")
                    println("Place agent .jar files in: ${marketplaceDir.absolutePath}")
                } else {
                    println("Available agents (${available.size}):")
                    available.forEach { println("  ${it.name} v${it.version} by ${it.author} — ${it.description}") }
                }
            }
            line.startsWith("install ", ignoreCase = true) -> {
                val name = line.removePrefix("install ").trim()
                val success = marketplace.install(name)
                if (success) {
                    println("Installed agent: $name")
                } else {
                    println("Could not install '$name'. Check marketplace directory.")
                }
            }
            line.equals("help", ignoreCase = true) -> {
                println("""
AgentOS CLI — Commands:

  Notes Agent:
    notes: <command>         create note, list notes, get note, search notes, delete note
    create note <title>      Quick create

  Calendar Agent:
    calendar: <command>      schedule, list events, cancel event
    schedule <event>         Quick schedule
    what's on <date>         Check calendar

  Weather Agent:
    weather <city>           Current weather
    forecast <city>          3-day forecast
    weather tomorrow         Tomorrow (last queried city)

  Tasks Agent:
    add task: <title> [priority: high/medium/low] [due: <date>]
    add task <title>         Quick add
    list tasks               Incomplete tasks
    all tasks                All tasks
    complete task <id>       Mark done
    delete task <id>         Remove task
    due today                Tasks due today
    due this week            Tasks due this week

  System:
    agents                   List registered agents
    marketplace / available  Browse marketplace
    install <name>           Install agent from marketplace
    help                     This message
    quit / exit              Exit
                """.trimIndent())
            }
            else -> {
                val response = routeMessage(line, notesAgent, calendarAgent, weatherAgent, tasksAgent)
                println(response)
            }
        }
        println()
    }

    AgentOS.shutdown()
}

private fun routeMessage(
    input: String,
    notesAgent: NotesAgent,
    calendarAgent: CalendarAgent,
    weatherAgent: WeatherAgent,
    tasksAgent: TasksAgent
): String = runBlocking {
    val lower = input.lowercase()

    // Weather routing
    if (lower.startsWith("weather ") || lower.startsWith("forecast ")) {
        return@runBlocking weatherAgent.onChat(ChatMessage("user", input)).text
    }

    // Tasks routing
    if (lower.startsWith("add task") || lower == "list tasks" || lower == "all tasks" ||
        lower.startsWith("complete task ") || lower.startsWith("delete task ") ||
        lower == "due today" || lower == "tasks due today" ||
        lower == "due this week" || lower == "tasks due this week") {
        return@runBlocking tasksAgent.onChat(ChatMessage("user", input)).text
    }

    // Notes routing
    if (lower.startsWith("notes:") || lower.startsWith("note ") ||
        lower.startsWith("create note") || lower == "list notes" ||
        lower.startsWith("search notes") || lower.startsWith("get note") ||
        lower.startsWith("delete note")) {
        val cmd = when {
            lower.startsWith("notes:") -> input.removePrefix("notes:").trim()
            lower.startsWith("note ") -> input.removePrefix("note ").trim()
            else -> input
        }
        return@runBlocking notesAgent.onChat(ChatMessage("user", cmd)).text
    }

    // Calendar routing
    if (lower.startsWith("calendar:")) {
        val cmd = input.removePrefix("calendar:").trim()
        return@runBlocking calendarAgent.onChat(ChatMessage("user", cmd)).text
    }

    if (lower.startsWith("schedule ") || lower.startsWith("what's on")) {
        return@runBlocking calendarAgent.onChat(ChatMessage("user", input)).text
    }

    "Unknown command. Type 'help' for available commands."
}
