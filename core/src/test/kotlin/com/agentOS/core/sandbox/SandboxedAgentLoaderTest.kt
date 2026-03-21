package com.agentOS.core.sandbox

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SandboxedAgentLoaderTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `getAgentSandboxDir creates directory`() {
        val loader = SandboxedAgentLoader(tempDir)
        val dir = loader.getAgentSandboxDir("com.test.agent")
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }

    @Test
    fun `getAgentSandboxDir for different agent IDs creates different directories`() {
        val loader = SandboxedAgentLoader(tempDir)
        val dir1 = loader.getAgentSandboxDir("com.agent.one")
        val dir2 = loader.getAgentSandboxDir("com.agent.two")
        assertNotEquals(dir1.absolutePath, dir2.absolutePath)
    }

    @Test
    fun `getAgentSandboxDir replaces dots with underscores`() {
        val loader = SandboxedAgentLoader(tempDir)
        val dir = loader.getAgentSandboxDir("com.test.agent")
        assertEquals("com_test_agent", dir.name)
    }
}
