package com.agentOS.core.marketplace

import com.agentOS.api.Agent
import com.agentOS.core.AgentRegistry
import com.agentOS.core.sandbox.AgentPackage
import com.agentOS.core.sandbox.SandboxedAgentLoader
import java.io.File

class LocalMarketplace(
    private val marketplaceDir: File,
    private val sandboxRoot: File,
    private val registry: AgentRegistry
) {
    private val loader = SandboxedAgentLoader(sandboxRoot)

    init {
        marketplaceDir.mkdirs()
        sandboxRoot.mkdirs()
    }

    /**
     * Scan marketplaceDir for .jar files with valid AgentPackage manifests.
     * Returns list of available packages.
     */
    fun listAvailable(): List<AgentPackage> {
        val jars = marketplaceDir.listFiles { file -> file.extension == "jar" } ?: return emptyList()
        return jars.mapNotNull { AgentPackage.load(it) }
    }

    /**
     * Install an agent from marketplace by name.
     * Finds the JAR, loads it, registers in AgentRegistry.
     */
    fun install(name: String): Boolean {
        val jars = marketplaceDir.listFiles { file -> file.extension == "jar" } ?: return false

        for (jar in jars) {
            val pkg = AgentPackage.load(jar) ?: continue
            if (pkg.name.equals(name, ignoreCase = true)) {
                return try {
                    val agent = loader.loadAgent(pkg, jar)
                    registry.register(agent)
                } catch (e: Exception) {
                    false
                }
            }
        }
        return false
    }

    /**
     * List installed agent IDs.
     */
    fun listInstalled(): List<String> {
        return registry.listAgents().map { it.id }
    }
}
