package com.antigravity.remote.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.antigravity.remote.ui.screen.ChatScreen
import com.antigravity.remote.ui.screen.DashboardScreen
import com.antigravity.remote.ui.screen.SettingsScreen
import com.antigravity.remote.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String) {
    data object WebRemote : Screen("web_remote", "网页版")
    data object Dashboard : Screen("dashboard", "会话列表")
    data object Chat : Screen("chat", "交互")
    data object Settings : Screen("settings", "设置")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val immersiveMode by viewModel.immersiveMode.collectAsState()

    val isFullscreenWeb = currentRoute == Screen.WebRemote.route && immersiveMode

    val items = listOf(Screen.WebRemote, Screen.Dashboard, Screen.Chat, Screen.Settings)

    Scaffold(
        bottomBar = {
            if (!isFullscreenWeb) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    Screen.WebRemote -> Icon(Icons.Filled.Home, contentDescription = screen.title)
                                    Screen.Dashboard -> Icon(Icons.Filled.Dashboard, contentDescription = screen.title)
                                    Screen.Chat -> Icon(Icons.Filled.Chat, contentDescription = screen.title)
                                    Screen.Settings -> Icon(Icons.Filled.Settings, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.WebRemote.route,
            modifier = Modifier.padding(if (isFullscreenWeb) PaddingValues(0.dp) else innerPadding)
        ) {
            composable(Screen.WebRemote.route) {
                com.antigravity.remote.ui.screen.WebRemoteScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onSessionClick = { sessionId ->
                        viewModel.selectSession(sessionId)
                        navController.navigate(Screen.Chat.route)
                    }
                )
            }
            composable(Screen.Chat.route) {
                ChatScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToWeb = {
                        navController.navigate(Screen.WebRemote.route)
                    }
                )
            }
        }
    }
}