package com.agentOS.android

import android.app.Application
import android.content.Context
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
import com.agentOS.core.storage.FileStorage
import java.io.File

class AgentOSApplication : Application() {

    private val prefs by lazy { getSharedPreferences("agentos_prefs", Context.MODE_PRIVATE) }

    var apiKey: String
        get() = prefs.getString("gemini_api_key", DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) {
            prefs.edit().putString("gemini_api_key", value.trim()).apply()
            reinitAgents()
        }

    var agents: Map<String, Agent> = emptyMap()
        private set

    override fun onCreate() {
        super.onCreate()
        reinitAgents()
    }

    fun reinitAgents() {
        val gemini = GeminiClient(apiKey = apiKey)
        val storageRoot = File(filesDir, "agent_data")

        fun scope(id: String, name: String, desc: String, caps: Set<String> = setOf("storage", "ui")) =
            AgentScope(id = id, name = name, version = "0.1.0", author = "Cameron", description = desc, capabilities = caps)

        fun storage(agentId: String, s: AgentScope) =
            FileStorage(s, File(storageRoot, agentId).also { it.mkdirs() })

        val notesScope    = scope("com.agentOS.notes",      "Notes Agent",     "Create, search, and link notes.")
        val calScope      = scope("com.agentOS.calendar",   "Calendar Agent",  "Natural language calendar.",         setOf("storage", "ui", "calendar", "notifications"))
        val tasksScope    = scope("com.agentOS.tasks",      "Tasks Agent",     "Task management with priorities and due dates.")
        val weatherScope  = scope("com.agentOS.weather",    "Weather Agent",   "Real-time weather via Open-Meteo API.")
        val emailScope    = scope("com.agentOS.email",      "Email Agent",     "Conversational email triage.",        setOf("storage", "ui", "network"))
        val msgScope      = scope("com.agentOS.messaging",  "Messaging Agent", "Universal inbox.",                   setOf("storage", "ui", "contacts"))
        val financeScope  = scope("com.agentOS.finance",    "Finance Agent",   "Budgeting and spending tracking.")

        agents = mapOf(
            "Notes"     to NotesAgent(    storage("notes",    notesScope),   gemini),
            "Calendar"  to CalendarAgent( storage("calendar", calScope),     gemini),
            "Tasks"     to TasksAgent(    storage("tasks",    tasksScope),   gemini),
            "Weather"   to WeatherAgent(  storage("weather",  weatherScope), gemini),
            "Email"     to EmailAgent(    storage("email",    emailScope),   gemini),
            "Messaging" to MessagingAgent(storage("messaging",msgScope),     gemini),
            "Finance"   to FinanceAgent(  storage("finance",  financeScope), gemini),
        )
    }

    companion object {
        private const val DEFAULT_API_KEY = "AIzaSyAPNvfGU6hlUAop3vE9BbJbukXKpvY6SB4"
    }
}
