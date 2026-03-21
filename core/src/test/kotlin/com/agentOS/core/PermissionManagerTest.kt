package com.agentOS.core

import com.agentOS.api.AgentScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PermissionManagerTest {

    private val scope = AgentScope(
        id = "test.perm",
        name = "Perm Test",
        version = "1.0.0",
        author = "Test",
        description = "Test",
        capabilities = setOf("storage", "ui"),
        optionalCapabilities = setOf("calendar")
    )

    private val pm = PermissionManager(scope)

    @Test
    fun `hasCapability returns true for required capability`() {
        assertTrue(pm.hasCapability("storage"))
        assertTrue(pm.hasCapability("ui"))
    }

    @Test
    fun `hasCapability returns true for optional capability`() {
        assertTrue(pm.hasCapability("calendar"))
    }

    @Test
    fun `hasCapability returns false for missing capability`() {
        assertFalse(pm.hasCapability("contacts"))
        assertFalse(pm.hasCapability("health"))
    }

    @Test
    fun `requireCapability succeeds for present capability`() {
        assertDoesNotThrow { pm.requireCapability("storage") }
        assertDoesNotThrow { pm.requireCapability("calendar") }
    }

    @Test
    fun `requireCapability throws PermissionDeniedException for missing capability`() {
        val ex = assertThrows<PermissionDeniedException> {
            pm.requireCapability("messaging")
        }
        assertTrue(ex.message!!.contains("messaging"))
        assertTrue(ex.message!!.contains("test.perm"))
    }
}
