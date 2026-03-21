package com.agentOS.core.sandbox

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class AgentPackageTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `load returns null for JAR without agent manifest attributes`() {
        val jarFile = File(tempDir, "plain.jar")
        val manifest = Manifest()
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        JarOutputStream(jarFile.outputStream(), manifest).use { it.finish() }

        val pkg = AgentPackage.load(jarFile)
        assertNull(pkg)
    }

    @Test
    fun `load returns AgentPackage for JAR with correct manifest`() {
        val jarFile = File(tempDir, "agent.jar")
        val manifest = Manifest()
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        manifest.mainAttributes.putValue("Agent-Name", "TestAgent")
        manifest.mainAttributes.putValue("Agent-Version", "1.0.0")
        manifest.mainAttributes.putValue("Agent-Main-Class", "com.test.TestAgent")
        manifest.mainAttributes.putValue("Agent-Capabilities", "storage, ui")
        manifest.mainAttributes.putValue("Agent-Author", "Tester")
        manifest.mainAttributes.putValue("Agent-Description", "A test agent")
        manifest.mainAttributes.putValue("Agent-Storage-Quota", "50000000")
        JarOutputStream(jarFile.outputStream(), manifest).use { it.finish() }

        val pkg = AgentPackage.load(jarFile)
        assertNotNull(pkg)
        assertEquals("TestAgent", pkg!!.name)
        assertEquals("1.0.0", pkg.version)
        assertEquals("com.test.TestAgent", pkg.mainClass)
        assertEquals(setOf("storage", "ui"), pkg.capabilities)
        assertEquals("Tester", pkg.author)
        assertEquals("A test agent", pkg.description)
        assertEquals(50_000_000L, pkg.storageQuotaBytes)
    }

    @Test
    fun `load returns null for non-existent file`() {
        val pkg = AgentPackage.load(File(tempDir, "nope.jar"))
        assertNull(pkg)
    }

    @Test
    fun `load uses defaults for optional attributes`() {
        val jarFile = File(tempDir, "minimal.jar")
        val manifest = Manifest()
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        manifest.mainAttributes.putValue("Agent-Name", "MinAgent")
        manifest.mainAttributes.putValue("Agent-Version", "0.1.0")
        manifest.mainAttributes.putValue("Agent-Main-Class", "com.test.MinAgent")
        manifest.mainAttributes.putValue("Agent-Capabilities", "storage")
        JarOutputStream(jarFile.outputStream(), manifest).use { it.finish() }

        val pkg = AgentPackage.load(jarFile)
        assertNotNull(pkg)
        assertEquals("Unknown", pkg!!.author)
        assertEquals("", pkg.description)
        assertEquals(100_000_000L, pkg.storageQuotaBytes)
    }
}
