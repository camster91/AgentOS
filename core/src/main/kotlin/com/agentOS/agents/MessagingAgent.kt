package com.agentOS.agents

import com.agentOS.ai.GeminiClient
import com.agentOS.api.Agent
import com.agentOS.api.AgentAPI
import com.agentOS.api.AgentScope
import com.agentOS.api.ChatMessage
import com.agentOS.api.StorageAPI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Message(
    val id: String,
    val contact: String,
    val body: String,
    val timestamp: Long,
    val isSent: Boolean
)

class MessagingAgent(private val storage: StorageAPI, private val gemini: GeminiClient) : Agent() {

    override val scope = AgentScope(
        id = "com.agentOS.messaging",
        name = "Messaging Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Universal inbox for SMS, messaging, and conversations.",
        capabilities = setOf("storage", "ui", "contacts")
    )

    override val api: AgentAPI = NoOpAgentAPI(storage)

    private val timeFmt = DateTimeFormatter.ofPattern("MMM d HH:mm")
        .withZone(ZoneId.systemDefault())

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val text = message.text.trim()
        val lower = text.lowercase()

        val response = when {
            lower.startsWith("send:") || lower.startsWith("send message:") -> {
                val rest = text.substringAfter(":").trimStart()
                sendMessage(rest)
            }
            lower == "list conversations" || lower == "conversations" || lower == "inbox" -> {
                listConversations()
            }
            lower.startsWith("read conversation ") -> {
                readConversation(text.substring(18).trim())
            }
            lower.startsWith("read messages ") || lower.startsWith("messages with ") -> {
                val contact = text.substringAfterLast(" ").trim()
                readConversation(contact)
            }
            lower.startsWith("delete message ") -> deleteMessage(text.substring(15).trim())
            lower.startsWith("search messages ") -> {
                val query = text.substring(16).trim()
                searchMessages(query)
            }
            lower.startsWith("contacts") || lower == "list contacts" -> listContacts()
            else -> aiChat(text)
        }
        return ChatMessage("assistant", response)
    }

    private suspend fun sendMessage(input: String): String {
        // Format: <contact> | <body>
        val parts = input.split("|", limit = 2)
        if (parts.size < 2) return "Format: send message: <contact> | <body>"
        val contact = parts[0].trim()
        val body = parts[1].trim()

        if (contact.isBlank()) return "Contact cannot be empty."
        if (body.isBlank()) return "Message body cannot be empty."

        val id = UUID.randomUUID().toString().take(8)
        val msg = Message(
            id = id,
            contact = contact,
            body = body,
            timestamp = System.currentTimeMillis(),
            isSent = true
        )
        storage.writeString("msg_$id", serializeMessage(msg))
        return "Message sent [ID: $id] To: $contact — \"${body.take(50)}${if (body.length > 50) "..." else ""}\""
    }

    private suspend fun listConversations(): String {
        val messages = getAllMessages()
        if (messages.isEmpty()) return "No conversations yet."

        // Group by contact, show latest message per contact
        val byContact = messages.groupBy { it.contact }
        val conversations = byContact.map { (contact, msgs) ->
            val latest = msgs.maxByOrNull { it.timestamp }!!
            contact to latest
        }.sortedByDescending { it.second.timestamp }

        return buildString {
            appendLine("Conversations (${conversations.size}):")
            for ((contact, latest) in conversations) {
                val time = timeFmt.format(Instant.ofEpochMilli(latest.timestamp))
                val direction = if (latest.isSent) "→" else "←"
                val preview = latest.body.take(40) + if (latest.body.length > 40) "..." else ""
                appendLine("  $contact  $direction \"$preview\" ($time)")
            }
        }.trimEnd()
    }

    private suspend fun readConversation(contact: String): String {
        val messages = getAllMessages()
            .filter { it.contact.equals(contact, ignoreCase = true) }
            .sortedBy { it.timestamp }

        if (messages.isEmpty()) return "No messages with \"$contact\"."

        return buildString {
            appendLine("Conversation with $contact (${messages.size} messages):")
            appendLine("─".repeat(40))
            for (msg in messages) {
                val time = timeFmt.format(Instant.ofEpochMilli(msg.timestamp))
                val sender = if (msg.isSent) "You" else contact
                appendLine("[$time] $sender: ${msg.body}")
            }
        }.trimEnd()
    }

    private suspend fun deleteMessage(id: String): String {
        val existing = storage.readString("msg_$id") ?: return "Message not found: $id"
        storage.delete("msg_$id")
        val msg = deserializeMessage(existing)
        return "Deleted message [${id}]${if (msg != null) ": \"${msg.body.take(40)}\"" else ""}"
    }

    private suspend fun searchMessages(query: String): String {
        if (query.isBlank()) return "Provide a search query."
        val matches = getAllMessages().filter { m ->
            m.body.contains(query, ignoreCase = true) ||
                m.contact.contains(query, ignoreCase = true)
        }.sortedByDescending { it.timestamp }

        if (matches.isEmpty()) return "No messages matching \"$query\"."
        return buildString {
            appendLine("Found ${matches.size} message(s) for \"$query\":")
            for (m in matches) {
                val time = timeFmt.format(Instant.ofEpochMilli(m.timestamp))
                val direction = if (m.isSent) "→ ${m.contact}" else "← ${m.contact}"
                appendLine("  [${m.id}] $direction ($time): \"${m.body.take(50)}\"")
            }
        }.trimEnd()
    }

    private suspend fun listContacts(): String {
        val contacts = getAllMessages().map { it.contact }.distinct().sorted()
        if (contacts.isEmpty()) return "No contacts yet."
        return "Contacts (${contacts.size}):\n" + contacts.joinToString("\n") { "  $it" }
    }

    private suspend fun getAllMessages(): List<Message> {
        return storage.listKeys()
            .filter { it.startsWith("msg_") }
            .mapNotNull { key -> storage.readString(key)?.let { deserializeMessage(it) } }
    }

    private fun serializeMessage(m: Message): String {
        val body = m.body.replace("\n", "\\n")
        return "${m.id}|${m.contact}|${m.timestamp}|${m.isSent}|$body"
    }

    private fun deserializeMessage(data: String): Message? {
        val parts = data.split("|", limit = 5)
        if (parts.size < 5) return null
        return try {
            Message(
                id = parts[0],
                contact = parts[1],
                timestamp = parts[2].toLong(),
                isSent = parts[3].toBoolean(),
                body = parts[4].replace("\\n", "\n")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun aiChat(userMessage: String): String {
        return gemini.chat(
            systemPrompt = """You are the Messaging Agent for AgentOS. You help users manage messages and conversations.
Available commands: send message: <contact> | <body>, list conversations, read conversation <contact>, delete message <id>, search messages <query>, list contacts.
Be helpful and concise.""",
            userMessage = userMessage
        )
    }
}
