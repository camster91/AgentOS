package com.agentOS.core

import com.agentOS.api.AgentScope
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AgentScopeTest {

    @Test
    fun `blank id throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            AgentScope(
                id = "",
                name = "Test Agent",
                version = "1.0.0",
                author = "Test Author",
                description = "A test agent"
            )
        }
    }

    @Test
    fun `cpuLimitPercent over 100 throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            AgentScope(
                id = "com.test.agent",
                name = "Test Agent",
                version = "1.0.0",
                author = "Test Author",
                description = "A test agent",
                cpuLimitPercent = 101
            )
        }
    }
}
