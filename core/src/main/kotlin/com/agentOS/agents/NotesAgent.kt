package com.agentOS.agents

import com.agentOS.api.*
import java.util.UUID

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val linkedNotes: List<String>
)

class NotesAgent(private val storage: StorageAPI) : Agent() {

    override val scope = AgentScope(
        id = "com.agentOS.notes",
        name = "Notes Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Create, search, and link notes via conversation.",
        capabilities = setOf("storage", "ui")
    )

    override val api: AgentAPI = NoOpAgentAPI(storage)

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val text = message.text.trim()
        val response = when {
            text.startsWith("create note:", ignoreCase = true) -> {
                val rest = text.removePrefix("create note:").trimStart()
                parseAndCreateNote(rest)
            }
            text.startsWith("create note ", ignoreCase = true) -> {
                val rest = text.removePrefix("create note ").trimStart()
                parseAndCreateNote(rest)
            }
            text.equals("list notes", ignoreCase = true) -> listNotes()
            text.startsWith("get note ", ignoreCase = true) -> {
                val query = text.removePrefix("get note ").trim()
                getNote(query)
            }
            text.startsWith("search notes ", ignoreCase = true) -> {
                val query = text.removePrefix("search notes ").trim()
                searchNotes(query)
            }
            text.startsWith("delete note ", ignoreCase = true) -> {
                val id = text.removePrefix("delete note ").trim()
                deleteNote(id)
            }
            else -> helpText()
        }
        return ChatMessage("assistant", response)
    }

    private suspend fun parseAndCreateNote(input: String): String {
        val parts = if (input.contains("|")) {
            input.split("|", limit = 2)
        } else if (input.contains(":")) {
            input.split(":", limit = 2)
        } else {
            return "Invalid format. Use: create note: <title> | <content> or create note <title>: <content>"
        }
        val title = parts[0].trim()
        val content = parts.getOrElse(1) { "" }.trim()
        if (title.isBlank()) return "Title cannot be empty."
        return createNote(title, content)
    }

    private suspend fun createNote(title: String, content: String): String {
        val id = UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()

        // Auto-link: find existing notes whose titles are mentioned in content
        val linkedNotes = mutableListOf<String>()
        val allNoteIds = storage.listKeys().filter { it.startsWith("note-") && !it.contains("-links") }
        for (key in allNoteIds) {
            val stored = storage.readString(key) ?: continue
            val existingNote = deserializeNote(stored) ?: continue
            if (content.contains(existingNote.title, ignoreCase = true)) {
                linkedNotes.add(existingNote.id)
            }
        }

        val note = Note(id, title, content, now, linkedNotes)
        storage.writeString("note-$id", serializeNote(note))

        val linkInfo = if (linkedNotes.isNotEmpty()) " Linked to: ${linkedNotes.joinToString(", ")}" else ""
        return "Note created [ID: $id] \"$title\"$linkInfo"
    }

    private suspend fun listNotes(): String {
        val keys = storage.listKeys().filter { it.startsWith("note-") && !it.contains("-links") }
        if (keys.isEmpty()) return "No notes found."
        val lines = keys.mapNotNull { key ->
            val stored = storage.readString(key) ?: return@mapNotNull null
            val note = deserializeNote(stored) ?: return@mapNotNull null
            "[${note.id}] ${note.title}"
        }
        return if (lines.isEmpty()) "No notes found." else lines.joinToString("\n")
    }

    private suspend fun getNote(query: String): String {
        // Try by ID first
        val byId = storage.readString("note-$query")
        if (byId != null) {
            val note = deserializeNote(byId)
            if (note != null) return formatNote(note)
        }

        // Try by title
        val keys = storage.listKeys().filter { it.startsWith("note-") && !it.contains("-links") }
        for (key in keys) {
            val stored = storage.readString(key) ?: continue
            val note = deserializeNote(stored) ?: continue
            if (note.title.equals(query, ignoreCase = true)) {
                return formatNote(note)
            }
        }

        return "Note not found: $query"
    }

    private suspend fun searchNotes(query: String): String {
        val keys = storage.listKeys().filter { it.startsWith("note-") && !it.contains("-links") }
        val matches = mutableListOf<Note>()
        for (key in keys) {
            val stored = storage.readString(key) ?: continue
            val note = deserializeNote(stored) ?: continue
            if (note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)) {
                matches.add(note)
            }
        }
        if (matches.isEmpty()) return "No notes matching \"$query\"."
        return matches.joinToString("\n") { "[${it.id}] ${it.title}" }
    }

    private suspend fun deleteNote(id: String): String {
        val existing = storage.readString("note-$id")
        return if (existing != null) {
            storage.delete("note-$id")
            "Note $id deleted."
        } else {
            "Note not found: $id"
        }
    }

    private fun formatNote(note: Note): String {
        val links = if (note.linkedNotes.isNotEmpty()) "\nLinked to: ${note.linkedNotes.joinToString(", ")}" else ""
        return "[${note.id}] ${note.title}\n${note.content}$links"
    }

    private fun serializeNote(note: Note): String {
        // Format: id|title|createdAt|linkedNoteIds|content
        // linkedNoteIds is comma-separated (or empty)
        val links = note.linkedNotes.joinToString(",")
        return "${note.id}|${note.title}|${note.createdAt}|$links|${note.content}"
    }

    private fun deserializeNote(data: String): Note? {
        val parts = data.split("|", limit = 5)
        if (parts.size < 5) return null
        val links = if (parts[3].isBlank()) emptyList() else parts[3].split(",")
        return Note(
            id = parts[0],
            title = parts[1],
            content = parts[4],
            createdAt = parts[2].toLongOrNull() ?: 0L,
            linkedNotes = links
        )
    }

    private fun helpText(): String {
        return """I don't understand that command. Available commands:
  create note: <title> | <content>
  create note <title>: <content>
  list notes
  get note <id or title>
  search notes <query>
  delete note <id>"""
    }
}
