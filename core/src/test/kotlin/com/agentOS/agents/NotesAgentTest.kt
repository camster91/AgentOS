package com.agentOS.agents

import com.agentOS.api.AgentScope
import com.agentOS.api.ChatMessage
import com.agentOS.core.storage.InMemoryStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NotesAgentTest {

    private lateinit var agent: NotesAgent

    private val testScope = AgentScope(
        id = "com.agentOS.notes.test",
        name = "Notes Test",
        version = "1.0.0",
        author = "Test",
        description = "Test notes agent",
        capabilities = setOf("storage", "ui")
    )

    @BeforeEach
    fun setUp() {
        agent = NotesAgent(InMemoryStorage(testScope))
    }

    private fun chat(text: String): String = runBlocking {
        agent.onChat(ChatMessage("user", text)).text
    }

    @Test
    fun `create note returns confirmation with ID`() {
        val response = chat("create note: Shopping | Buy milk and eggs")
        assertTrue(response.startsWith("Note created [ID:"), "Expected confirmation, got: $response")
        assertTrue(response.contains("Shopping"))
    }

    @Test
    fun `create note with colon separator`() {
        val response = chat("create note My Title: Some content here")
        assertTrue(response.startsWith("Note created [ID:"))
        assertTrue(response.contains("My Title"))
    }

    @Test
    fun `list notes returns created notes`() {
        chat("create note: Alpha | Content A")
        chat("create note: Beta | Content B")
        val response = chat("list notes")
        assertTrue(response.contains("Alpha"), "Expected Alpha in: $response")
        assertTrue(response.contains("Beta"), "Expected Beta in: $response")
    }

    @Test
    fun `list notes when empty`() {
        val response = chat("list notes")
        assertEquals("No notes found.", response)
    }

    @Test
    fun `get note by ID`() {
        val createResponse = chat("create note: Lookup Test | Important content")
        // Extract ID from "Note created [ID: xxxxxxxx] ..."
        val id = Regex("""\[ID: (\w+)]""").find(createResponse)?.groupValues?.get(1)
        assertNotNull(id, "Could not extract ID from: $createResponse")

        val response = chat("get note $id")
        assertTrue(response.contains("Lookup Test"), "Expected title in: $response")
        assertTrue(response.contains("Important content"), "Expected content in: $response")
    }

    @Test
    fun `get note by title`() {
        chat("create note: Findable | Some stuff")
        val response = chat("get note Findable")
        assertTrue(response.contains("Findable"))
        assertTrue(response.contains("Some stuff"))
    }

    @Test
    fun `search notes finds by content`() {
        chat("create note: Recipe | Chocolate cake with frosting")
        chat("create note: Todo | Fix the fence")
        val response = chat("search notes chocolate")
        assertTrue(response.contains("Recipe"), "Expected Recipe in: $response")
        assertFalse(response.contains("Todo"), "Should not contain Todo in: $response")
    }

    @Test
    fun `search notes finds by title`() {
        chat("create note: Kotlin Tips | Some tips")
        chat("create note: Java Tips | Other tips")
        val response = chat("search notes Kotlin")
        assertTrue(response.contains("Kotlin Tips"))
        assertFalse(response.contains("Java Tips"))
    }

    @Test
    fun `delete note removes it`() {
        val createResponse = chat("create note: Deletable | Gone soon")
        val id = Regex("""\[ID: (\w+)]""").find(createResponse)?.groupValues?.get(1)!!

        val deleteResponse = chat("delete note $id")
        assertTrue(deleteResponse.contains("deleted"), "Expected deleted in: $deleteResponse")

        val listResponse = chat("list notes")
        assertEquals("No notes found.", listResponse)
    }

    @Test
    fun `auto-linking creates links when content references other notes`() {
        // Create note A
        chat("create note: Project Alpha | This is project alpha details")

        // Create note B that mentions "Project Alpha" in content
        val responseB = chat("create note: Status Update | Progress on Project Alpha is good")

        assertTrue(responseB.contains("Linked to:"), "Expected auto-link in: $responseB")
    }

    @Test
    fun `unknown command returns help text`() {
        val response = chat("do something random")
        assertTrue(response.contains("I don't understand"))
        assertTrue(response.contains("create note"))
    }
}
