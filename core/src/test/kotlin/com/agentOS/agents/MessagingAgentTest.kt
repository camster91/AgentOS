package com.agentOS.agents

import com.agentOS.ai.GeminiClient
import com.agentOS.api.AgentScope
import com.agentOS.api.ChatMessage
import com.agentOS.core.storage.InMemoryStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MessagingAgentTest {

    private lateinit var agent: MessagingAgent

    private val testScope = AgentScope(
        id = "com.agentOS.messaging.test",
        name = "Messaging Test",
        version = "1.0.0",
        author = "Test",
        description = "Test messaging agent",
        capabilities = setOf("storage", "ui", "contacts")
    )

    @BeforeEach
    fun setUp() {
        agent = MessagingAgent(InMemoryStorage(testScope), GeminiClient(apiKey = "test-key"))
    }

    private fun chat(text: String): String = runBlocking {
        agent.onChat(ChatMessage("user", text)).text
    }

    @Test
    fun `send message returns confirmation with ID`() {
        val response = chat("send message: Alice | Hey, how are you?")
        assertTrue(response.startsWith("Message sent [ID:"), "Expected confirmation, got: $response")
        assertTrue(response.contains("Alice"))
    }

    @Test
    fun `send message missing body returns format hint`() {
        val response = chat("send message: Alice")
        assertTrue(response.contains("Format:"), "Expected format hint, got: $response")
    }

    @Test
    fun `send message empty body returns error`() {
        val response = chat("send message: Alice | ")
        assertTrue(response.contains("empty") || response.contains("Format:"))
    }

    @Test
    fun `list conversations when empty`() {
        val response = chat("list conversations")
        assertEquals("No conversations yet.", response)
    }

    @Test
    fun `sent message appears in conversations`() {
        chat("send message: Bob | Hello Bob!")
        val response = chat("list conversations")
        assertTrue(response.contains("Bob"), "Expected Bob in: $response")
        assertTrue(response.contains("Hello Bob"))
    }

    @Test
    fun `read conversation shows messages`() {
        chat("send message: Carol | First message")
        chat("send message: Carol | Second message")
        val response = chat("read conversation Carol")
        assertTrue(response.contains("First message"))
        assertTrue(response.contains("Second message"))
        assertTrue(response.contains("Carol"))
    }

    @Test
    fun `read conversation unknown contact returns message`() {
        val response = chat("read conversation Nobody")
        assertTrue(response.contains("No messages with"), "Expected no messages, got: $response")
    }

    @Test
    fun `search messages finds by body`() {
        chat("send message: Dave | Meeting at 3pm today")
        chat("send message: Eve | Nothing important here")
        val response = chat("search messages meeting")
        assertTrue(response.contains("Dave") || response.contains("Meeting"))
        assertFalse(response.contains("Nothing important"))
    }

    @Test
    fun `list contacts shows all unique contacts`() {
        chat("send message: Frank | Hi")
        chat("send message: Grace | Hello")
        chat("send message: Frank | Follow up")
        val response = chat("list contacts")
        assertTrue(response.contains("Frank"))
        assertTrue(response.contains("Grace"))
        // Frank appears twice but should be listed once
        assertEquals(1, response.split("Frank").size - 1, "Frank should appear once")
    }

    @Test
    fun `delete message removes it`() {
        val sendResponse = chat("send message: Hank | Delete this")
        val id = Regex("""\[ID: (\w+)]""").find(sendResponse)?.groupValues?.get(1)!!

        val deleteResponse = chat("delete message $id")
        assertTrue(deleteResponse.contains("Deleted"), "Expected deleted, got: $deleteResponse")
    }

    @Test
    fun `unknown command falls back to AI response`() {
        val response = chat("what is the meaning of life")
        assertTrue(response.startsWith("AI unavailable"), "Expected AI fallback, got: $response")
    }
}
