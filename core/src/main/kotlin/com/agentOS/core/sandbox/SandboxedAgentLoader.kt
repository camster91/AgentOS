package com.agentOS.core.sandbox

import com.agentOS.api.Agent
import java.io.File
import java.net.URLClassLoader

class SandboxedAgentLoader(private val sandboxRoot: File) {

    /**
     * Load an agent class from a JAR file using an isolated URLClassLoader.
     * Each agent gets its own ClassLoader to prevent class pollution between agents.
     *
     * @param agentPackage The package manifest describing the agent
     * @param jarFile The JAR file containing the agent
     * @return Instantiated Agent object
     * @throws IllegalArgumentException if mainClass doesn't extend Agent
     */
    fun loadAgent(agentPackage: AgentPackage, jarFile: File): Agent {
        val classLoader = URLClassLoader(
            arrayOf(jarFile.toURI().toURL()),
            this::class.java.classLoader
        )

        val clazz = classLoader.loadClass(agentPackage.mainClass)

        require(Agent::class.java.isAssignableFrom(clazz)) {
            "Class ${agentPackage.mainClass} does not extend Agent"
        }

        @Suppress("UNCHECKED_CAST")
        val agentClass = clazz as Class<out Agent>
        return agentClass.getDeclaredConstructor().newInstance()
    }

    /**
     * Get the sandbox directory for a specific agent.
     * Each agent gets its own subdirectory: sandboxRoot/<agentId>/
     */
    fun getAgentSandboxDir(agentId: String): File {
        val dir = File(sandboxRoot, agentId.replace(".", "_"))
        dir.mkdirs()
        return dir
    }
}
