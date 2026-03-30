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

data class Email(
    val id: String,
    val from: String,
    val to: String,
    val subject: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean,
    val folder: String = "inbox"
)

class EmailAgent(private val storage: StorageAPI, private val gemini: GeminiClient) : Agent() {

    override val scope = AgentScope(
        id = "com.agentOS.email",
        name = "Email Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Conversational email triage, compose, and organize.",
        capabilities = setOf("storage", "ui", "network")
    )

    override val api: AgentAPI = NoOpAgentAPI(storage)

    private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val text = message.text.trim()
        val lower = text.lowercase()

        val response = when {
            lower.startsWith("compose:") || lower.startsWith("compose email:") -> {
                val rest = text.substringAfter(":").trimStart()
                composeEmail(rest)
            }
            lower == "list inbox" || lower == "inbox" -> listFolder("inbox")
            lower == "list sent" || lower == "sent" -> listFolder("sent")
            lower.startsWith("read email ") -> readEmail(text.substring(11).trim())
            lower.startsWith("read ") && lower.contains("email") -> readEmail(lower.substringAfterLast(" "))
            lower.startsWith("search emails ") || lower.startsWith("search email ") -> {
                val query = text.substringAfter(" ", "").substringAfter(" ", "").trim()
                searchEmails(query)
            }
            lower.startsWith("delete email ") -> deleteEmail(text.substring(13).trim())
            lower.startsWith("mark read ") -> markRead(text.substring(10).trim())
            lower.startsWith("reply to ") -> {
                val rest = text.substring(9).trim()
                replyTo(rest)
            }
            lower == "unread" || lower == "unread emails" -> listUnread()
            else -> aiChat(text)
        }
        return ChatMessage("assistant", response)
    }

    private suspend fun composeEmail(input: String): String {
        // Format: <to> | <subject> | <body>
        val parts = input.split("|", limit = 3)
        if (parts.size < 2) {
            return "Format: compose email: <to> | <subject> | <body>"
        }
        val to = parts[0].trim()
        val subject = parts[1].trim()
        val body = parts.getOrElse(2) { "" }.trim()

        if (to.isBlank()) return "Recipient (to) cannot be empty."
        if (subject.isBlank()) return "Subject cannot be empty."

        val id = UUID.randomUUID().toString().take(8)
        val email = Email(
            id = id,
            from = "me@agentos.local",
            to = to,
            subject = subject,
            body = body,
            timestamp = System.currentTimeMillis(),
            isRead = true,
            folder = "sent"
        )
        storage.writeString("email_$id", serializeEmail(email))
        return "Email sent [ID: $id] To: $to | Subject: \"$subject\""
    }

    private suspend fun listFolder(folder: String): String {
        val emails = getAllEmails().filter { it.folder == folder }
            .sortedByDescending { it.timestamp }
        if (emails.isEmpty()) return "No emails in $folder."
        return buildString {
            appendLine("${folder.replaceFirstChar { it.uppercase() }} (${emails.size}):")
            for (e in emails) {
                val unread = if (!e.isRead && folder == "inbox") " [UNREAD]" else ""
                val date = dateFmt.format(Instant.ofEpochMilli(e.timestamp))
                appendLine("  [${e.id}]$unread ${e.subject} — ${if (folder == "sent") "To: ${e.to}" else "From: ${e.from}"} ($date)")
            }
        }.trimEnd()
    }

    private suspend fun listUnread(): String {
        val emails = getAllEmails().filter { it.folder == "inbox" && !it.isRead }
            .sortedByDescending { it.timestamp }
        if (emails.isEmpty()) return "No unread emails."
        return buildString {
            appendLine("Unread (${emails.size}):")
            for (e in emails) {
                val date = dateFmt.format(Instant.ofEpochMilli(e.timestamp))
                appendLine("  [${e.id}] ${e.subject} — From: ${e.from} ($date)")
            }
        }.trimEnd()
    }

    private suspend fun readEmail(idOrSubject: String): String {
        val email = findEmail(idOrSubject) ?: return "Email not found: $idOrSubject"
        // Mark as read
        if (!email.isRead) {
            storage.writeString("email_${email.id}", serializeEmail(email.copy(isRead = true)))
        }
        val date = dateFmt.format(Instant.ofEpochMilli(email.timestamp))
        return buildString {
            appendLine("From: ${email.from}")
            appendLine("To: ${email.to}")
            appendLine("Subject: ${email.subject}")
            appendLine("Date: $date")
            appendLine("---")
            append(email.body.ifBlank { "(no body)" })
        }.trimEnd()
    }

    private suspend fun searchEmails(query: String): String {
        if (query.isBlank()) return "Provide a search query."
        val matches = getAllEmails().filter { e ->
            e.subject.contains(query, ignoreCase = true) ||
                e.body.contains(query, ignoreCase = true) ||
                e.from.contains(query, ignoreCase = true) ||
                e.to.contains(query, ignoreCase = true)
        }.sortedByDescending { it.timestamp }
        if (matches.isEmpty()) return "No emails matching \"$query\"."
        return buildString {
            appendLine("Found ${matches.size} email(s) for \"$query\":")
            for (e in matches) {
                appendLine("  [${e.id}] [${e.folder}] ${e.subject} — From: ${e.from}")
            }
        }.trimEnd()
    }

    private suspend fun deleteEmail(idOrSubject: String): String {
        val email = findEmail(idOrSubject) ?: return "Email not found: $idOrSubject"
        storage.delete("email_${email.id}")
        return "Deleted email [${email.id}]: \"${email.subject}\""
    }

    private suspend fun markRead(idOrSubject: String): String {
        val email = findEmail(idOrSubject) ?: return "Email not found: $idOrSubject"
        storage.writeString("email_${email.id}", serializeEmail(email.copy(isRead = true)))
        return "Marked as read: \"${email.subject}\""
    }

    private suspend fun replyTo(input: String): String {
        // Format: <id> | <body>
        val parts = input.split("|", limit = 2)
        val idOrSubject = parts[0].trim()
        val body = parts.getOrElse(1) { "" }.trim()
        if (body.isBlank()) return "Format: reply to <id> | <body>"

        val original = findEmail(idOrSubject) ?: return "Email not found: $idOrSubject"
        val id = UUID.randomUUID().toString().take(8)
        val reply = Email(
            id = id,
            from = "me@agentos.local",
            to = original.from,
            subject = "Re: ${original.subject}",
            body = body,
            timestamp = System.currentTimeMillis(),
            isRead = true,
            folder = "sent"
        )
        storage.writeString("email_$id", serializeEmail(reply))
        return "Reply sent [ID: $id] To: ${original.from} | Subject: \"Re: ${original.subject}\""
    }

    private suspend fun findEmail(query: String): Email? {
        val all = getAllEmails()
        return all.find { it.id == query }
            ?: all.find { it.subject.contains(query, ignoreCase = true) }
    }

    private suspend fun getAllEmails(): List<Email> {
        return storage.listKeys()
            .filter { it.startsWith("email_") }
            .mapNotNull { key -> storage.readString(key)?.let { deserializeEmail(it) } }
    }

    private fun serializeEmail(e: Email): String {
        val body = e.body.replace("\n", "\\n")
        return "${e.id}|${e.from}|${e.to}|${e.subject}|${e.timestamp}|${e.isRead}|${e.folder}|$body"
    }

    private fun deserializeEmail(data: String): Email? {
        val parts = data.split("|", limit = 8)
        if (parts.size < 8) return null
        return try {
            Email(
                id = parts[0],
                from = parts[1],
                to = parts[2],
                subject = parts[3],
                timestamp = parts[4].toLong(),
                isRead = parts[5].toBoolean(),
                folder = parts[6],
                body = parts[7].replace("\\n", "\n")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun aiChat(userMessage: String): String {
        return gemini.chat(
            systemPrompt = """You are the Email Agent for AgentOS. You help users manage email via conversation.
Available commands: compose email: <to> | <subject> | <body>, list inbox, list sent, read email <id>, search emails <query>, delete email <id>, mark read <id>, reply to <id> | <body>, unread.
Be helpful and concise.""",
            userMessage = userMessage
        )
    }
}
