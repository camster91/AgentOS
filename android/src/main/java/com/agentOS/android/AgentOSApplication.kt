package com.agentOS.android

import android.app.Application
import com.agentOS.agents.CalendarAgent
import com.agentOS.agents.EmailAgent
import com.agentOS.agents.FinanceAgent
import com.agentOS.agents.MessagingAgent
import com.agentOS.agents.NotesAgent
import com.agentOS.agents.TasksAgent
import com.agentOS.agents.WeatherAgent
import com.agentOS.ai.GeminiClient
import com.agentOS.api.Agent
import com.agentOS.api.AgentScope
import com.agentOS.core.storage.InMemoryStorage

class AgentOSApplication : Application() {

    lateinit var agents: Map<String, Agent>
        private set

    override fun onCreate() {
        super.onCreate()

        val gemini = GeminiClient(
            apiKey = System.getenv("GEMINI_API_KEY") ?: "AIzaSyAPNvfGU6hlUAop3vE9BbJbukXKpvY6SB4"
        )

        fun scope(id: String, name: String, desc: String, caps: Set<String> = setOf("storage", "ui")) =
            AgentScope(id = id, name = name, version = "0.1.0", author = "Cameron", description = desc, capabilities = caps)

        agents = mapOf(
            "Notes" to NotesAgent(
                InMemoryStorage(scope("com.agentOS.notes", "Notes Agent", "Create, search, and link notes.")),
                gemini
            ),
            "Calendar" to CalendarAgent(
                InMemoryStorage(scope("com.agentOS.calendar", "Calendar Agent", "Natural language calendar.", setOf("storage", "ui", "calendar", "notifications"))),
                gemini
            ),
            "Tasks" to TasksAgent(
                InMemoryStorage(scope("com.agentOS.tasks", "Tasks Agent", "Task management with priorities and due dates.")),
                gemini
            ),
            "Weather" to WeatherAgent(
                InMemoryStorage(scope("com.agentOS.weather", "Weather Agent", "Real-time weather via Open-Meteo API.")),
                gemini
            ),
            "Email" to EmailAgent(
                InMemoryStorage(scope("com.agentOS.email", "Email Agent", "Conversational email triage.", setOf("storage", "ui", "network"))),
                gemini
            ),
            "Messaging" to MessagingAgent(
                InMemoryStorage(scope("com.agentOS.messaging", "Messaging Agent", "Universal inbox.", setOf("storage", "ui", "contacts"))),
                gemini
            ),
            "Finance" to FinanceAgent(
                InMemoryStorage(scope("com.agentOS.finance", "Finance Agent", "Budgeting and spending tracking.")),
                gemini
            ),
        )
    }
}
