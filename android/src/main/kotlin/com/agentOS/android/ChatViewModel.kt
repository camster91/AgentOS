package com.agentOS.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    val agentList: List<String> = listOf("Notes", "Calendar", "Tasks", "Weather")

    private val _selectedAgent = MutableStateFlow("Notes")
    val selectedAgent: StateFlow<String> = _selectedAgent.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    fun selectAgent(agent: String) {
        _selectedAgent.value = agent
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            sender = "You",
            text = text,
            isUser = true,
        )
        _messages.value = _messages.value + userMessage

        val agent = _selectedAgent.value
        _isTyping.value = true

        viewModelScope.launch {
            delay(500)
            val response = ChatMessage(
                sender = "$agent Agent",
                text = "$agent agent: Got your message!",
                isUser = false,
            )
            _messages.value = _messages.value + response
            _isTyping.value = false
        }
    }
}
