package com.agentOS.sync

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class SyncEntry(
    val userId: String,
    val agentId: String,
    val key: String,
    val value: String,
    val timestamp: Long
)

val store = mutableMapOf<String, MutableList<SyncEntry>>()

fun Application.configureSyncRoutes() {
    install(ContentNegotiation) {
        gson()
    }

    routing {
        post("/sync/push") {
            val entry = call.receive<SyncEntry>()
            val bucket = "${entry.userId}:${entry.agentId}"
            store.getOrPut(bucket) { mutableListOf() }.add(entry)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        get("/sync/pull") {
            val userId = call.request.queryParameters["userId"] ?: ""
            val agentId = call.request.queryParameters["agentId"] ?: ""
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L

            val bucket = "$userId:$agentId"
            val entries = store[bucket]?.filter { it.timestamp >= since } ?: emptyList()
            call.respond(entries)
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080) {
        configureSyncRoutes()
    }.start(wait = true)
}
