package com.antigravity.remote.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.remote.ui.navigation.AppNavigation
import com.antigravity.remote.ui.theme.AntigravityRemoteTheme
import com.antigravity.remote.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val darkTheme by viewModel.darkTheme.collectAsState()
            AntigravityRemoteTheme(darkTheme = darkTheme) {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}