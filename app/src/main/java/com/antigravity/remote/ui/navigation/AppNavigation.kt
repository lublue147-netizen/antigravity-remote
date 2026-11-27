package com.antigravity.remote.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
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
import com.antigravity.remote.ui.dialog.UpdateDialog
import com.antigravity.remote.ui.dialog.VersionPickerDialog
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

    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val selectedReleaseForDialog by viewModel.selectedReleaseForDialog.collectAsState()
    val showVersionPickerDialog by viewModel.showVersionPickerDialog.collectAsState()
    val availableReleases by viewModel.availableReleases.collectAsState()
    val isLoadingReleases by viewModel.isLoadingReleases.collectAsState()

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
        val isWebRemoteTab = currentRoute == null || currentRoute == Screen.WebRemote.route
        Box(
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (isFullscreenWeb) 0.dp else innerPadding.calculateBottomPadding()
                )
                .imePadding()
        ) {
            // Keep WebRemoteScreen mounted across tab switches so WebView is paused/resumed
            // instead of destroyed and reloaded on every tab switch.
            com.antigravity.remote.ui.screen.WebRemoteScreen(
                viewModel = viewModel,
                isActiveTab = isWebRemoteTab,
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

            NavHost(
                navController = navController,
                startDestination = Screen.WebRemote.route
            ) {
                composable(Screen.WebRemote.route) {
                    // Rendered by the persistent WebRemoteScreen above
                }
                composable(Screen.Dashboard.route) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onSessionClick = { sessionId ->
                                viewModel.selectSession(sessionId)
                                navController.navigate(Screen.Chat.route)
                            }
                        )
                    }
                }
                composable(Screen.Chat.route) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        ChatScreen(viewModel = viewModel)
                    }
                }
                composable(Screen.Settings.route) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToWeb = {
                                navController.navigate(Screen.WebRemote.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // In-app Update Dialog
    if (showUpdateDialog && selectedReleaseForDialog != null) {
        UpdateDialog(
            release = selectedReleaseForDialog!!,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onSelectOtherVersions = { viewModel.fetchAllReleases() }
        )
    }

    // Version Picker Dialog for selecting specific release to install
    if (showVersionPickerDialog) {
        VersionPickerDialog(
            releases = availableReleases,
            isLoading = isLoadingReleases,
            onSelectRelease = { release ->
                viewModel.selectReleaseForUpdate(release)
            },
            onDismiss = { viewModel.dismissVersionPickerDialog() },
            onRefresh = { viewModel.fetchAllReleases() }
        )
    }
}