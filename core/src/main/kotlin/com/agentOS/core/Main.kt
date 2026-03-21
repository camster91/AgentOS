package com.agentOS.core

import com.agentOS.agents.CalendarAgent
import com.agentOS.agents.NotesAgent
import com.agentOS.api.ChatMessage
import com.agentOS.core.storage.InMemoryStorage
import com.agentOS.api.AgentScope
import kotlinx.coroutines.runBlocking

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

    val notesAgent = NotesAgent(InMemoryStorage(notesScope))
    val calendarAgent = CalendarAgent(InMemoryStorage(calendarScope))

    AgentOS.registry.register(notesAgent)
    AgentOS.registry.register(calendarAgent)

    println()
    println("Welcome to AgentOS!")
    println("Agents loaded: ${AgentOS.registry.listAgents().joinToString(", ") { it.name }}")
    println("Type 'help' for routing info, 'quit' to exit.")
    println()

    while (true) {
        print("> ")
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
            line.equals("help", ignoreCase = true) -> {
                println("""
AgentOS CLI — Routing:
  notes: <command>    → Notes Agent (create, list, get, search, delete)
  note <command>      → Notes Agent
  calendar: <command> → Calendar Agent
  schedule <...>      → Calendar Agent
  what's on <...>     → Calendar Agent
  agents              → List registered agents
  help                → This message
  quit / exit         → Exit
                """.trimIndent())
            }
            else -> {
                val response = routeMessage(line, notesAgent, calendarAgent)
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
    calendarAgent: CalendarAgent
): String = runBlocking {
    val lower = input.lowercase()

    // Explicit routing
    if (lower.startsWith("notes:") || lower.startsWith("note ")) {
        val cmd = if (lower.startsWith("notes:")) input.removePrefix("notes:").trim()
                  else input.removePrefix("note ").trim()
        return@runBlocking notesAgent.onChat(ChatMessage("user", cmd)).text
    }

    if (lower.startsWith("calendar:")) {
        val cmd = input.removePrefix("calendar:").trim()
        return@runBlocking calendarAgent.onChat(ChatMessage("user", cmd)).text
    }

    if (lower.startsWith("schedule ") || lower.startsWith("what's on ")) {
        return@runBlocking calendarAgent.onChat(ChatMessage("user", input)).text
    }

    // Fallback: try notes first, then calendar
    val notesResponse = notesAgent.onChat(ChatMessage("user", input)).text
    if (!notesResponse.contains("I don't understand")) {
        return@runBlocking notesResponse
    }

    val calendarResponse = calendarAgent.onChat(ChatMessage("user", input)).text
    if (!calendarResponse.contains("I don't understand")) {
        return@runBlocking calendarResponse
    }

    notesResponse
}
