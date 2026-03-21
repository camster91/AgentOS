package com.agentOS.android

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agentOS.android.ui.ChatScreen
import com.agentOS.android.ui.SettingsScreen
import com.agentOS.android.ui.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("chat") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }
        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
