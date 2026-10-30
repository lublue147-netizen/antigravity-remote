package com.antigravity.remote.ui

import android.os.Bundle
import android.webkit.CookieManager
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

        // Power-saving optimization: cap display refresh rate to 60Hz to eliminate 90Hz/120Hz GPU and screen drain
        try {
            val params = window.attributes
            params.preferredRefreshRate = 60f
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val modes = display?.supportedModes
                val mode60 = modes?.firstOrNull { kotlin.math.abs(it.refreshRate - 60f) < 1f }
                if (mode60 != null) {
                    params.preferredDisplayModeId = mode60.modeId
                }
            }
            window.attributes = params
        } catch (_: Exception) {}

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val darkTheme by viewModel.darkTheme.collectAsState()
            AntigravityRemoteTheme(darkTheme = darkTheme) {
                AppNavigation(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            CookieManager.getInstance().flush()
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            CookieManager.getInstance().flush()
        } catch (_: Exception) {}
    }
}