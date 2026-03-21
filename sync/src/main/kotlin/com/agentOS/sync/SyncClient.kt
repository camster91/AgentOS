package com.agentOS.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SyncClient(val serverUrl: String = "http://10.0.2.2:8080") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    fun push(userId: String, agentId: String, key: String, value: String): Boolean {
        return try {
            val entry = SyncEntry(
                userId = userId,
                agentId = agentId,
                key = key,
                value = value,
                timestamp = System.currentTimeMillis()
            )
            val body = gson.toJson(entry).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$serverUrl/sync/push")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    fun pull(userId: String, agentId: String, since: Long = 0L): List<SyncEntry> {
        return try {
            val request = Request.Builder()
                .url("$serverUrl/sync/pull?userId=$userId&agentId=$agentId&since=$since")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val type = object : TypeToken<List<SyncEntry>>() {}.type
                    gson.fromJson(response.body?.string() ?: "[]", type)
                } else {
                    emptyList()
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
