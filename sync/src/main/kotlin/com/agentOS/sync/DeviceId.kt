package com.agentOS.sync

import java.io.File
import java.util.UUID

object DeviceId {
    fun get(storageDir: String = System.getProperty("user.home") + "/.agentOS"): String {
        val dir = File(storageDir)
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "device-id.txt")
        return if (file.exists()) {
            file.readText().trim()
        } else {
            val id = UUID.randomUUID().toString()
            file.writeText(id)
            id
        }
    }
}
