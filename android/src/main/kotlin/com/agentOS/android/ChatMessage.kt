package com.agentOS.android

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUser: Boolean,
)
