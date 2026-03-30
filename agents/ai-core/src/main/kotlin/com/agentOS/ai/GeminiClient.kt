package com.agentOS.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatTurn(val role: String, val content: String)

class GeminiClient(
    val apiKey: String,
    val model: String = "gemini-2.0-flash"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun chat(
        systemPrompt: String,
        userMessage: String,
        history: List<ChatTurn> = emptyList()
    ): String {
        if (apiKey.isBlank()) return "AI unavailable — no API key configured. Add your Gemini key in Settings."

        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val contents = mutableListOf<Map<String, Any>>()
            for (turn in history) {
                contents.add(mapOf(
                    "role" to turn.role,
                    "parts" to listOf(mapOf("text" to turn.content))
                ))
            }
            contents.add(mapOf(
                "role" to "user",
                "parts" to listOf(mapOf("text" to userMessage))
            ))

            val body = mapOf(
                "system_instruction" to mapOf(
                    "parts" to listOf(mapOf("text" to systemPrompt))
                ),
                "contents" to contents
            )

            val requestBody = gson.toJson(body).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return "AI unavailable — empty response."

            val json = gson.fromJson(responseBody, JsonObject::class.java)

            // Check for API-level error (e.g. invalid key, quota exceeded)
            json.getAsJsonObject("error")?.let { err ->
                val msg = err.get("message")?.asString ?: "Unknown API error"
                val status = err.get("status")?.asString ?: ""
                return when {
                    status == "UNAUTHENTICATED" -> "AI unavailable — invalid API key. Update it in Settings."
                    status == "RESOURCE_EXHAUSTED" -> "AI unavailable — API quota exceeded. Try again later."
                    else -> "AI unavailable — $msg"
                }
            }

            val candidates = json.getAsJsonArray("candidates")
                ?: return "AI unavailable — unexpected response format."
            if (candidates.size() == 0) return "AI unavailable — no response generated."

            candidates.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
                ?: "AI unavailable — could not parse response."

        } catch (e: java.net.UnknownHostException) {
            "AI unavailable — no internet connection."
        } catch (e: java.net.SocketTimeoutException) {
            "AI unavailable — request timed out."
        } catch (e: Exception) {
            "AI unavailable — ${e.message}"
        }
    }
}
