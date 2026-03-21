package com.agentOS.core.sandbox

import java.io.File
import java.util.jar.JarFile

data class AgentPackage(
    val name: String,
    val version: String,
    val mainClass: String,
    val capabilities: Set<String>,
    val author: String = "Unknown",
    val description: String = "",
    val storageQuotaBytes: Long = 100_000_000
) {
    companion object {
        /**
         * Load an AgentPackage from a JAR file by reading its manifest.
         * JAR manifest attributes:
         * - Agent-Name: <name>
         * - Agent-Version: <version>
         * - Agent-Main-Class: <fully qualified class name>
         * - Agent-Capabilities: <comma-separated list>
         * - Agent-Author: <author>
         * - Agent-Description: <description>
         * - Agent-Storage-Quota: <bytes>
         *
         * Returns null if the JAR doesn't have required agent manifest attributes.
         */
        fun load(jarFile: File): AgentPackage? {
            if (!jarFile.exists() || !jarFile.name.endsWith(".jar")) return null

            return try {
                JarFile(jarFile).use { jar ->
                    val manifest = jar.manifest ?: return null
                    val attrs = manifest.mainAttributes

                    val name = attrs.getValue("Agent-Name") ?: return null
                    val version = attrs.getValue("Agent-Version") ?: return null
                    val mainClass = attrs.getValue("Agent-Main-Class") ?: return null
                    val capabilitiesStr = attrs.getValue("Agent-Capabilities") ?: return null

                    val capabilities = capabilitiesStr.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()

                    val author = attrs.getValue("Agent-Author") ?: "Unknown"
                    val description = attrs.getValue("Agent-Description") ?: ""
                    val storageQuota = attrs.getValue("Agent-Storage-Quota")?.toLongOrNull()
                        ?: 100_000_000

                    AgentPackage(
                        name = name,
                        version = version,
                        mainClass = mainClass,
                        capabilities = capabilities,
                        author = author,
                        description = description,
                        storageQuotaBytes = storageQuota
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
