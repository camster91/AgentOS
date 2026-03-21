package com.agentOS.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.agentOS.android.ui.AgentOSTheme
import com.agentOS.android.ui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentOSTheme {
                HomeScreen()
            }
        }
    }
}
