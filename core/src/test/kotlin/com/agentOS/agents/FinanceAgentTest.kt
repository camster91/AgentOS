package com.agentOS.agents

import com.agentOS.ai.GeminiClient
import com.agentOS.api.AgentScope
import com.agentOS.api.ChatMessage
import com.agentOS.core.storage.InMemoryStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FinanceAgentTest {

    private lateinit var agent: FinanceAgent

    private val testScope = AgentScope(
        id = "com.agentOS.finance.test",
        name = "Finance Test",
        version = "1.0.0",
        author = "Test",
        description = "Test finance agent",
        capabilities = setOf("storage", "ui")
    )

    @BeforeEach
    fun setUp() {
        agent = FinanceAgent(InMemoryStorage(testScope), GeminiClient(apiKey = "test-key"))
    }

    private fun chat(text: String): String = runBlocking {
        agent.onChat(ChatMessage("user", text)).text
    }

    @Test
    fun `add expense returns confirmation with ID`() {
        val response = chat("add expense: Coffee | 4.50 | Food")
        assertTrue(response.startsWith("Transaction recorded [ID:"), "Expected confirmation, got: $response")
        assertTrue(response.contains("Coffee"))
        assertTrue(response.contains("Food"))
    }

    @Test
    fun `add income stores positive amount`() {
        val response = chat("add income: Salary | 3000 | Income")
        assertTrue(response.startsWith("Transaction recorded [ID:"))
        assertTrue(response.contains("+"))
        assertTrue(response.contains("Salary"))
    }

    @Test
    fun `add transaction with missing amount returns error`() {
        val response = chat("add transaction: No amount | notanumber")
        assertTrue(response.contains("Invalid amount") || response.contains("Format:"),
            "Expected error, got: $response")
    }

    @Test
    fun `list transactions when empty`() {
        val response = chat("list transactions")
        assertEquals("No transactions recorded.", response)
    }

    @Test
    fun `list transactions shows added entries`() {
        chat("add expense: Groceries | 50.00 | Food")
        chat("add expense: Gas | 40.00 | Transport")
        val response = chat("list transactions")
        assertTrue(response.contains("Groceries"))
        assertTrue(response.contains("Gas"))
    }

    @Test
    fun `budget summary shows balance`() {
        chat("add income: Freelance | 1000 | Income")
        chat("add expense: Rent | 500 | Housing")
        val response = chat("budget summary")
        assertTrue(response.contains("Total Income"))
        assertTrue(response.contains("Total Expenses"))
        assertTrue(response.contains("Balance"))
    }

    @Test
    fun `budget summary when empty`() {
        val response = chat("budget summary")
        assertTrue(response.contains("Budget Summary"))
        assertTrue(response.contains("$0.00"))
    }

    @Test
    fun `set budget stores category limit`() {
        val response = chat("set budget: Food | 300")
        assertTrue(response.contains("Food"))
        assertTrue(response.contains("300") || response.contains("\$300"))
    }

    @Test
    fun `set budget appears in budget summary`() {
        chat("set budget: Food | 300")
        val response = chat("budget summary")
        assertTrue(response.contains("Food"), "Expected Food budget in summary: $response")
    }

    @Test
    fun `spending by category groups correctly`() {
        chat("add expense: Lunch | 12 | Food")
        chat("add expense: Dinner | 25 | Food")
        chat("add expense: Bus fare | 3 | Transport")
        val response = chat("spending by category")
        assertTrue(response.contains("Food"))
        assertTrue(response.contains("Transport"))
        // Food should be larger amount
        val foodIndex = response.indexOf("Food")
        val transportIndex = response.indexOf("Transport")
        assertTrue(foodIndex < transportIndex, "Food ($37) should appear before Transport ($3)")
    }

    @Test
    fun `delete transaction removes it`() {
        val addResponse = chat("add expense: Mistake | 99 | Misc")
        val id = Regex("""\[ID: (\w+)]""").find(addResponse)?.groupValues?.get(1)!!

        val deleteResponse = chat("delete transaction $id")
        assertTrue(deleteResponse.contains("Deleted"), "Expected deleted, got: $deleteResponse")

        val listResponse = chat("list transactions")
        assertFalse(listResponse.contains("Mistake"))
    }

    @Test
    fun `unknown command falls back to AI response`() {
        val response = chat("what should I invest in")
        assertTrue(response.startsWith("AI unavailable"), "Expected AI fallback, got: $response")
    }
}
