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

    private val _selectedAgent = MutableStateFlow("Notes")
    val selectedAgent: StateFlow<String> = _selectedAgent.asStateFlow()

    // Per-agent message history
    private val agentHistories = mutableMapOf<String, MutableList<ChatMessage>>()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _showMarketplace = MutableStateFlow(false)
    val showMarketplace: StateFlow<Boolean> = _showMarketplace.asStateFlow()

    fun openMarketplace() { _showMarketplace.value = true }
    fun closeMarketplace() { _showMarketplace.value = false }

    fun selectAgent(agent: String) {
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
                val agent = app.agents[agentName]
                if (agent != null) {
                    agent.onChat(AgentChatMessage("user", text)).text
                } else {
                    "Agent \"$agentName\" is not available."
                }
            } catch (e: Exception) {
                "Error: ${e.message ?: "Something went wrong."}"
            }

            val response = ChatMessage(sender = "$agentName Agent", text = responseText, isUser = false)
            history.add(response)
            _messages.value = history.toList()
            _isTyping.value = false
        }
    }
}
