package com.agentOS.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentOS.android.ui.AgentOSTheme
import com.agentOS.android.ui.ChatScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentOSTheme {
                val chatViewModel: ChatViewModel = viewModel()
                ChatScreen(viewModel = chatViewModel)
            }
        }
    }
}
