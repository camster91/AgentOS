package com.agentOS.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentOS.android.AgentOSApplication

private data class AgentInfo(val name: String, val description: String)

private val allAgents = listOf(
    AgentInfo("Notes",     "Create, search, and link notes with auto-linking"),
    AgentInfo("Calendar",  "Schedule events with natural language"),
    AgentInfo("Tasks",     "Track to-dos with priorities and due dates"),
    AgentInfo("Weather",   "Real-time weather via Open-Meteo (no key needed)"),
    AgentInfo("Email",     "Compose, triage, and organize email"),
    AgentInfo("Messaging", "Universal inbox for SMS and messaging"),
    AgentInfo("Finance",   "Track spending, income, and budgets"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as AgentOSApplication
    val keyboard = LocalSoftwareKeyboardController.current

    var apiKeyInput by remember { mutableStateOf(app.apiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = Color.White,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // ── AI Configuration ──────────────────────────────────────────
            Text(
                text = "AI Configuration",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gemini API Key",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    saved = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your Gemini API key", color = Color.White.copy(alpha = 0.4f)) },
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (apiKeyVisible) "Hide key" else "Show key",
                            tint = Color.White.copy(alpha = 0.6f),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                ),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Get a free key at aistudio.google.com",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    app.apiKey = apiKeyInput
                    saved = true
                    keyboard?.hide()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKeyInput.isNotBlank() && (apiKeyInput != app.apiKey || !saved),
            ) {
                Text(if (saved && apiKeyInput == app.apiKey) "Saved ✓" else "Save API Key")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color.White.copy(alpha = 0.1f))

            // ── Agents ────────────────────────────────────────────────────
            Text(
                text = "Agents (${allAgents.size})",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            allAgents.forEach { agent ->
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)) {
                    Text(text = agent.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(text = agent.description, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.1f))

            // ── About ─────────────────────────────────────────────────────
            Text(
                text = "About",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "AgentOS \u2014 AI agents for Android", color = Color.White, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "v0.3.0", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "7 built-in agents · Gemini 2.0 Flash", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
    }
}
