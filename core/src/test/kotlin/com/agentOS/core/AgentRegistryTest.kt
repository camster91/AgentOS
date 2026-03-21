package com.agentOS.core

import com.agentOS.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AgentRegistryTest {

    private lateinit var registry: AgentRegistry

    private fun createTestAgent(id: String): Agent {
        return object : Agent() {
            override val scope = AgentScope(
                id = id,
                name = "Agent $id",
                version = "1.0.0",
                author = "Test",
                description = "Test agent $id",
                capabilities = setOf("storage")
            )
            override val api: AgentAPI get() = throw UnsupportedOperationException("Not used in test")
            override suspend fun onChat(message: ChatMessage) = ChatMessage("assistant", "reply")
        }
    }

    @BeforeEach
    fun setUp() {
        registry = AgentRegistry()
    }

    @Test
    fun `register and getAgent`() {
        val agent = createTestAgent("a1")
        assertTrue(registry.register(agent))
        assertNotNull(registry.getAgent("a1"))
        assertEquals("a1", registry.getAgent("a1")!!.scope.id)
    }

    @Test
    fun `duplicate register returns false`() {
        val agent = createTestAgent("dup")
        assertTrue(registry.register(agent))
        assertFalse(registry.register(agent))
    }

    @Test
    fun `unregister removes agent`() {
        val agent = createTestAgent("rm")
        registry.register(agent)
        assertTrue(registry.unregister("rm"))
        assertNull(registry.getAgent("rm"))
    }

    @Test
    fun `unregister non-existent returns false`() {
        assertFalse(registry.unregister("nope"))
    }

    @Test
    fun `listAgents returns metadata for all agents`() {
        registry.register(createTestAgent("x"))
        registry.register(createTestAgent("y"))
        val list = registry.listAgents()
        assertEquals(2, list.size)
        val ids = list.map { it.id }.sorted()
        assertEquals(listOf("x", "y"), ids)
    }

    @Test
    fun `getCount returns correct count`() {
        assertEquals(0, registry.getCount())
        registry.register(createTestAgent("one"))
        assertEquals(1, registry.getCount())
        registry.register(createTestAgent("two"))
        assertEquals(2, registry.getCount())
    }
}
