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

    internal val prefs by lazy { getSharedPreferences("agentos_prefs", Context.MODE_PRIVATE) }

    var apiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) {
            prefs.edit().putString("gemini_api_key", value.trim()).apply()
            reinitAgents()
        }

    /** True if the user has never saved an API key (first launch). */
    val isFirstLaunch: Boolean
        get() = prefs.getString("gemini_api_key", null) == null

    var agents: Map<String, Agent> = emptyMap()
        private set

    private val historyDir: File by lazy { File(filesDir, "chat_history").also { it.mkdirs() } }

    override fun onCreate() {
        super.onCreate()
        reinitAgents()
    }

    fun reinitAgents() {
        val key = apiKey.ifBlank { DEFAULT_API_KEY }
        val gemini = GeminiClient(apiKey = key)
        val storageRoot = File(filesDir, "agent_data")

        fun scope(id: String, name: String, desc: String, caps: Set<String> = setOf("storage", "ui")) =
            AgentScope(id = id, name = name, version = "0.1.0", author = "Cameron", description = desc, capabilities = caps)

        fun storage(agentId: String, s: AgentScope) =
            FileStorage(s, File(storageRoot, agentId).also { it.mkdirs() })

        val notesScope   = scope("com.agentOS.notes",     "Notes Agent",     "Create, search, and link notes.")
        val calScope     = scope("com.agentOS.calendar",  "Calendar Agent",  "Natural language calendar.",        setOf("storage", "ui", "calendar", "notifications"))
        val tasksScope   = scope("com.agentOS.tasks",     "Tasks Agent",     "Task management with priorities and due dates.")
        val weatherScope = scope("com.agentOS.weather",   "Weather Agent",   "Real-time weather via Open-Meteo API.")
        val emailScope   = scope("com.agentOS.email",     "Email Agent",     "Conversational email triage.",       setOf("storage", "ui", "network"))
        val msgScope     = scope("com.agentOS.messaging", "Messaging Agent", "Universal inbox.",                  setOf("storage", "ui", "contacts"))
        val financeScope = scope("com.agentOS.finance",   "Finance Agent",   "Budgeting and spending tracking.")

        agents = mapOf(
            "Notes"     to NotesAgent(    storage("notes",     notesScope),   gemini),
            "Calendar"  to CalendarAgent( storage("calendar",  calScope),     gemini),
            "Tasks"     to TasksAgent(    storage("tasks",     tasksScope),   gemini),
            "Weather"   to WeatherAgent(  storage("weather",   weatherScope), gemini),
            "Email"     to EmailAgent(    storage("email",     emailScope),   gemini),
            "Messaging" to MessagingAgent(storage("messaging", msgScope),     gemini),
            "Finance"   to FinanceAgent(  storage("finance",  financeScope),  gemini),
        )
    }

    // ── Chat history persistence ──────────────────────────────────────────────

    fun loadChatHistory(agentName: String): List<ChatMessage> {
        val file = File(historyDir, "${agentName.lowercase()}.txt")
        if (!file.exists()) return emptyList()
        return file.readLines()
            .mapNotNull { deserializeMessage(it) }
            .takeLast(MAX_HISTORY)
    }

    fun saveChatHistory(agentName: String, messages: List<ChatMessage>) {
        val file = File(historyDir, "${agentName.lowercase()}.txt")
        val lines = messages.takeLast(MAX_HISTORY).joinToString("\n") { serializeMessage(it) }
        file.writeText(lines)
    }

    private fun serializeMessage(m: ChatMessage): String {
        val escapedText = m.text.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n")
        val escapedSender = m.sender.replace("|", "\\|")
        return "${m.timestamp}|${m.isUser}|$escapedSender|$escapedText"
    }

    private fun deserializeMessage(line: String): ChatMessage? {
        if (line.isBlank()) return null
        // Split on unescaped | only (first 3 delimiters)
        val parts = line.split(Regex("(?<!\\\\)\\|"), limit = 4)
        if (parts.size < 4) return null
        return try {
            ChatMessage(
                timestamp = parts[0].toLong(),
                isUser = parts[1].toBoolean(),
                sender = parts[2].replace("\\|", "|"),
                text = parts[3].replace("\\n", "\n").replace("\\|", "|").replace("\\\\", "\\"),
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val DEFAULT_API_KEY = "AIzaSyAPNvfGU6hlUAop3vE9BbJbukXKpvY6SB4"
        private const val MAX_HISTORY = 100
    }
}
