package com.agentOS.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentOS.android.ChatMessage
import com.agentOS.android.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val messages by viewModel.messages.collectAsState()
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val showMarketplace by viewModel.showMarketplace.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    if (showMarketplace) {
        AlertDialog(
            onDismissRequest = { viewModel.closeMarketplace() },
            title = { Text("Agent Marketplace") },
            text = {
                Text(
                    "The marketplace is coming soon!\n\n" +
                    "You'll be able to install community-built agents — Finance, Fitness, Travel, and more — directly from here.\n\n" +
                    "Stay tuned for v0.4.0."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeMarketplace() }) {
                    Text("Got it")
                }
            },
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = { Text("AgentOS") },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = Color.White,
            ),
        )

        AgentSelectorBar(
            agents = viewModel.agentList,
            selectedAgent = selectedAgent,
            onAgentSelected = { viewModel.selectAgent(it) },
            onAddAgent = { viewModel.openMarketplace() },
        )

        if (messages.isEmpty() && !isTyping) {
            AgentEmptyState(
                agentName = selectedAgent,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
                if (isTyping) {
                    item {
                        Text(
                            text = "${selectedAgent} Agent is typing…",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }

        MessageInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                viewModel.sendMessage(inputText)
                inputText = ""
            },
        )
    }
}

private val agentHints = mapOf(
    "Notes" to listOf(
        "create note: Meeting | Discussed Q1 roadmap",
        "list notes",
        "search notes quarterly",
        "get note <id or title>",
    ),
    "Calendar" to listOf(
        "schedule Dentist on Friday at 2pm",
        "list events",
        "list events tomorrow",
        "cancel event <id or title>",
    ),
    "Tasks" to listOf(
        "add task: Review PR priority: high due: tomorrow",
        "list tasks",
        "due today",
        "complete task <id or title>",
    ),
    "Weather" to listOf(
        "weather New York",
        "forecast London",
        "weather tomorrow",
    ),
    "Email" to listOf(
        "compose email: alice@example.com | Hello | Hi there!",
        "list inbox",
        "unread",
        "reply to <id> | Sure, see you then!",
    ),
    "Messaging" to listOf(
        "send message: Alice | Hey, free for lunch?",
        "list conversations",
        "read conversation Alice",
        "search messages lunch",
    ),
    "Finance" to listOf(
        "add expense: Coffee | 4.50 | Food",
        "add income: Freelance | 500 | Income",
        "budget summary",
        "spending by category",
    ),
)

@Composable
private fun AgentEmptyState(agentName: String, modifier: Modifier = Modifier) {
    val hints = agentHints[agentName] ?: listOf("Ask me anything about $agentName")
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$agentName Agent",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try a command or just chat naturally:",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        hints.forEach { hint ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = hint,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) Color(0xFF6C63FF) else Color(0xFF1A1A1A)
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        if (!isUser) {
            Text(
                text = message.sender,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    ),
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.text,
                color = Color.White,
                fontSize = 15.sp,
            )
        }
        Text(
            text = timeFormat.format(Date(message.timestamp)),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message…") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
        )
        IconButton(onClick = { if (text.isNotBlank()) onSend() }) {
            Text(
                text = "➤",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
            )
        }
    }
}
