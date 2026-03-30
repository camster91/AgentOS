package com.agentOS.agents

import com.agentOS.ai.GeminiClient
import com.agentOS.api.AgentScope
import com.agentOS.api.ChatMessage
import com.agentOS.core.storage.InMemoryStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmailAgentTest {

    private lateinit var agent: EmailAgent

    private val testScope = AgentScope(
        id = "com.agentOS.email.test",
        name = "Email Test",
        version = "1.0.0",
        author = "Test",
        description = "Test email agent",
        capabilities = setOf("storage", "ui", "network")
    )

    @BeforeEach
    fun setUp() {
        agent = EmailAgent(InMemoryStorage(testScope), GeminiClient(apiKey = "test-key"))
    }

    private fun chat(text: String): String = runBlocking {
        agent.onChat(ChatMessage("user", text)).text
    }

    @Test
    fun `compose email returns confirmation with ID`() {
        val response = chat("compose email: alice@example.com | Hello | Hi there!")
        assertTrue(response.startsWith("Email sent [ID:"), "Expected confirmation, got: $response")
        assertTrue(response.contains("alice@example.com"))
        assertTrue(response.contains("Hello"))
    }

    @Test
    fun `compose email missing body is allowed`() {
        val response = chat("compose email: bob@test.com | Subject only |")
        assertTrue(response.startsWith("Email sent [ID:"))
    }

    @Test
    fun `compose email missing parts returns format hint`() {
        val response = chat("compose email: recipient-only")
        assertTrue(response.contains("Format:"), "Expected format hint, got: $response")
    }

    @Test
    fun `list inbox when empty`() {
        val response = chat("list inbox")
        assertEquals("No emails in inbox.", response)
    }

    @Test
    fun `sent email appears in sent folder`() {
        chat("compose email: alice@example.com | Test | Body text")
        val response = chat("list sent")
        assertTrue(response.contains("Test"), "Expected email in sent: $response")
        assertTrue(response.contains("alice@example.com"))
    }

    @Test
    fun `read email by ID marks as read`() {
        val composeResponse = chat("compose email: alice@example.com | Read Test | Body content")
        val id = Regex("""\[ID: (\w+)]""").find(composeResponse)?.groupValues?.get(1)
        assertNotNull(id, "Could not extract ID from: $composeResponse")

        val readResponse = chat("read email $id")
        assertTrue(readResponse.contains("Read Test"))
        assertTrue(readResponse.contains("Body content"))
        assertTrue(readResponse.contains("alice@example.com"))
    }

    @Test
    fun `read email not found returns message`() {
        val response = chat("read email nonexistent")
        assertTrue(response.contains("not found"), "Expected not found, got: $response")
    }

    @Test
    fun `search emails finds by subject`() {
        chat("compose email: a@b.com | Important Notice | Please read")
        chat("compose email: c@d.com | Unrelated | Nothing special")
        val response = chat("search emails Important")
        assertTrue(response.contains("Important Notice"))
        assertFalse(response.contains("Unrelated"))
    }

    @Test
    fun `delete email removes it`() {
        val composeResponse = chat("compose email: x@y.com | Delete Me | Gone")
        val id = Regex("""\[ID: (\w+)]""").find(composeResponse)?.groupValues?.get(1)!!

        val deleteResponse = chat("delete email $id")
        assertTrue(deleteResponse.contains("Deleted"), "Expected deleted, got: $deleteResponse")

        val listResponse = chat("list sent")
        assertFalse(listResponse.contains("Delete Me"))
    }

    @Test
    fun `unread shows no unread when inbox is empty`() {
        val response = chat("unread")
        assertEquals("No unread emails.", response)
    }

    @Test
    fun `unknown command falls back to AI response`() {
        val response = chat("do something random with email")
        assertTrue(response.startsWith("AI unavailable"), "Expected AI fallback, got: $response")
    }
}
