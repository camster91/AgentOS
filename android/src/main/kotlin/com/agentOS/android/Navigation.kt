package com.agentOS.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agentOS.android.ui.ChatScreen
import com.agentOS.android.ui.OnboardingScreen
import com.agentOS.android.ui.SettingsScreen
import com.agentOS.android.ui.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()
    val app = LocalContext.current.applicationContext as AgentOSApplication

    val startDestination = if (app.isFirstLaunch) "onboarding" else "splash"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onFinished = {
                    navController.navigate("chat") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }
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
                onNavigateToSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
