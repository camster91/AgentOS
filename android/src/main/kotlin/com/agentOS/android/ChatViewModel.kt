package com.agentOS.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentOS.api.ChatMessage as AgentChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AgentOSApplication

    val agentList: List<String> = listOf("Notes", "Calendar", "Tasks", "Weather", "Email", "Messaging", "Finance")

    private val _selectedAgent = MutableStateFlow(agentList.first())
    val selectedAgent: StateFlow<String> = _selectedAgent.asStateFlow()

    // Per-agent in-memory history (backed by file storage)
    private val agentHistories = mutableMapOf<String, MutableList<ChatMessage>>()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _showMarketplace = MutableStateFlow(false)
    val showMarketplace: StateFlow<Boolean> = _showMarketplace.asStateFlow()

    init {
        // Pre-load history for the first agent
        loadHistoryFor(agentList.first())
        _messages.value = agentHistories.getOrPut(agentList.first()) { mutableListOf() }.toList()
    }

    fun selectAgent(agent: String) {
        if (!agentHistories.containsKey(agent)) loadHistoryFor(agent)
        _selectedAgent.value = agent
        _messages.value = agentHistories.getOrPut(agent) { mutableListOf() }.toList()
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val agentName = _selectedAgent.value
        val history = agentHistories.getOrPut(agentName) { mutableListOf() }

        val userMessage = ChatMessage(sender = "You", text = text, isUser = true)
        history.add(userMessage)
        _messages.value = history.toList()
        _isTyping.value = true

        viewModelScope.launch {
            val responseText = try {
                app.agents[agentName]?.onChat(AgentChatMessage("user", text))?.text
                    ?: "Agent \"$agentName\" is not available."
            } catch (e: Exception) {
                "Error: ${e.message ?: "Something went wrong."}"
            }

            val response = ChatMessage(sender = "$agentName Agent", text = responseText, isUser = false)
            history.add(response)
            _messages.value = history.toList()
            _isTyping.value = false

            // Persist after every exchange
            app.saveChatHistory(agentName, history)
        }
    }

    fun openMarketplace() { _showMarketplace.value = true }
    fun closeMarketplace() { _showMarketplace.value = false }

    private fun loadHistoryFor(agentName: String) {
        val loaded = app.loadChatHistory(agentName).toMutableList()
        agentHistories[agentName] = loaded
    }
}
