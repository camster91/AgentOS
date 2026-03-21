package com.agentOS.agents

import com.agentOS.ai.GeminiClient
import com.agentOS.api.Agent
import com.agentOS.api.AgentAPI
import com.agentOS.api.AgentScope
import com.agentOS.api.ChatMessage
import com.agentOS.api.StorageAPI
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WeatherAgent(private val storage: StorageAPI, private val gemini: GeminiClient) : Agent() {

    override val scope = AgentScope(
        id = "com.agentOS.weather",
        name = "Weather Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "Real-time weather via Open-Meteo API. No API key needed.",
        capabilities = setOf("storage", "ui")
    )

    override val api: AgentAPI = NoOpAgentAPI(storage)

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val text = message.text.trim()
        val lower = text.lowercase()

        val response = when {
            lower.startsWith("weather tomorrow") -> {
                val lastCity = storage.readString("last_city")
                if (lastCity != null) {
                    getForecastForDay(lastCity, 1)
                } else {
                    "No city set yet. Use 'weather <city>' first."
                }
            }
            lower.startsWith("weather ") -> {
                val city = text.removePrefix(text.substring(0, 8)).trim()
                getCurrentWeather(city)
            }
            lower.startsWith("forecast ") -> {
                val city = text.removePrefix(text.substring(0, 9)).trim()
                getForecast(city)
            }
            else -> aiChat(text)
        }
        return ChatMessage("assistant", response)
    }

    private suspend fun getCurrentWeather(city: String): String {
        val coords = getCoordinates(city) ?: return "Could not find coordinates for '$city'."
        storage.writeString("last_city", city)

        val weatherJson = fetchWeather(coords.first, coords.second)
            ?: return "Could not fetch weather data. Please try again later."

        val temp = extractJsonValue(weatherJson, "temperature", inObject = "current_weather")
        val wind = extractJsonValue(weatherJson, "windspeed", inObject = "current_weather")

        return "Current weather in $city: ${temp}°C, Wind ${wind} km/h"
    }

    private suspend fun getForecast(city: String): String {
        val coords = getCoordinates(city) ?: return "Could not find coordinates for '$city'."
        storage.writeString("last_city", city)

        val weatherJson = fetchWeather(coords.first, coords.second)
            ?: return "Could not fetch weather data. Please try again later."

        val maxTemps = extractJsonArray(weatherJson, "temperature_2m_max")
        val minTemps = extractJsonArray(weatherJson, "temperature_2m_min")

        if (maxTemps.size < 3 || minTemps.size < 3) {
            return "Could not parse forecast data."
        }

        return buildString {
            append("3-day forecast for $city:")
            append("\n  Today: ${minTemps[0]}-${maxTemps[0]}°C")
            append("\n  Tomorrow: ${minTemps[1]}-${maxTemps[1]}°C")
            append("\n  Day 3: ${minTemps[2]}-${maxTemps[2]}°C")
        }
    }

    private suspend fun getForecastForDay(city: String, dayIndex: Int): String {
        val coords = getCoordinates(city) ?: return "Could not find coordinates for '$city'."

        val weatherJson = fetchWeather(coords.first, coords.second)
            ?: return "Could not fetch weather data. Please try again later."

        val maxTemps = extractJsonArray(weatherJson, "temperature_2m_max")
        val minTemps = extractJsonArray(weatherJson, "temperature_2m_min")

        if (maxTemps.size <= dayIndex || minTemps.size <= dayIndex) {
            return "Could not parse forecast data."
        }

        return "Tomorrow's weather in $city: ${minTemps[dayIndex]}-${maxTemps[dayIndex]}°C"
    }

    private suspend fun getCoordinates(city: String): Pair<String, String>? {
        // Check cache first
        val cachedLat = storage.readString("geo_lat_$city")
        val cachedLon = storage.readString("geo_lon_$city")
        if (cachedLat != null && cachedLon != null) {
            return Pair(cachedLat, cachedLon)
        }

        val encoded = URLEncoder.encode(city, "UTF-8")
        val urlStr = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1"

        val json = httpGet(urlStr, mapOf("User-Agent" to "AgentOS/0.1.0")) ?: return null

        val lat = extractFirstArrayObjectValue(json, "lat") ?: return null
        val lon = extractFirstArrayObjectValue(json, "lon") ?: return null

        // Cache coordinates
        storage.writeString("geo_lat_$city", lat)
        storage.writeString("geo_lon_$city", lon)

        return Pair(lat, lon)
    }

    private fun fetchWeather(lat: String, lon: String): String? {
        val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current_weather=true&daily=temperature_2m_max,temperature_2m_min,weathercode" +
                "&timezone=auto&forecast_days=7"
        return httpGet(urlStr)
    }

    private fun httpGet(urlStr: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

            if (connection.responseCode != 200) return null

            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract a value from within a named JSON object.
     * e.g., extractJsonValue(json, "temperature", inObject = "current_weather")
     * finds "current_weather":{..."temperature":15.2...} and returns "15.2"
     */
    private fun extractJsonValue(json: String, key: String, inObject: String? = null): String {
        val searchIn = if (inObject != null) {
            val objStart = json.indexOf("\"$inObject\"")
            if (objStart == -1) return "?"
            val braceStart = json.indexOf("{", objStart)
            if (braceStart == -1) return "?"
            // Find matching closing brace
            var depth = 0
            var end = braceStart
            for (i in braceStart until json.length) {
                when (json[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) { end = i; break }
                    }
                }
            }
            json.substring(braceStart, end + 1)
        } else {
            json
        }

        val keyIdx = searchIn.indexOf("\"$key\"")
        if (keyIdx == -1) return "?"
        val colonIdx = searchIn.indexOf(":", keyIdx + key.length + 2)
        if (colonIdx == -1) return "?"

        val afterColon = searchIn.substring(colonIdx + 1).trimStart()
        // Value could be number, string, etc.
        val valueEnd = afterColon.indexOfFirst { it == ',' || it == '}' || it == ']' }
        val raw = if (valueEnd == -1) afterColon.trim() else afterColon.substring(0, valueEnd).trim()
        return raw.trim('"')
    }

    /**
     * Extract a JSON array of numbers by key name.
     * e.g., "temperature_2m_max":[12.5,14.0,11.2,...]
     */
    private fun extractJsonArray(json: String, key: String): List<String> {
        val keyIdx = json.indexOf("\"$key\"")
        if (keyIdx == -1) return emptyList()
        val bracketStart = json.indexOf("[", keyIdx)
        if (bracketStart == -1) return emptyList()
        val bracketEnd = json.indexOf("]", bracketStart)
        if (bracketEnd == -1) return emptyList()

        val arrayContent = json.substring(bracketStart + 1, bracketEnd)
        return arrayContent.split(",").map { it.trim().trim('"') }
    }

    /**
     * Extract a value from the first object in a JSON array.
     * e.g., [{"lat":"43.65","lon":"-79.38",...}] → extracts "lat" → "43.65"
     */
    private fun extractFirstArrayObjectValue(json: String, key: String): String? {
        val firstBrace = json.indexOf("{")
        if (firstBrace == -1) return null
        val closingBrace = json.indexOf("}", firstBrace)
        if (closingBrace == -1) return null

        val obj = json.substring(firstBrace, closingBrace + 1)
        val keyIdx = obj.indexOf("\"$key\"")
        if (keyIdx == -1) return null
        val colonIdx = obj.indexOf(":", keyIdx)
        if (colonIdx == -1) return null

        val afterColon = obj.substring(colonIdx + 1).trimStart()
        val valueEnd = afterColon.indexOfFirst { it == ',' || it == '}' }
        val raw = if (valueEnd == -1) afterColon.trim() else afterColon.substring(0, valueEnd).trim()
        return raw.trim('"')
    }

    private fun aiChat(userMessage: String): String {
        return gemini.chat(
            systemPrompt = """You are the Weather Agent for AgentOS. You help users check weather and forecasts.
Available commands you can suggest: weather <city>, forecast <city>, weather tomorrow.
Be helpful and concise. If the user's intent maps to a command, tell them the exact command to use.""",
            userMessage = userMessage
        )
    }

    private fun helpText(): String = """Weather Agent — Commands:
  weather <city>      — Current weather for a city
  forecast <city>     — 3-day forecast for a city
  weather tomorrow    — Tomorrow's weather (uses last queried city)"""
}
