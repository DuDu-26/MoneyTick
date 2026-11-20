// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dudu26.moneytick.core.ThemeMode
import io.github.dudu26.moneytick.data.settingsRepository
import io.github.dudu26.moneytick.overlay.FloatingWindowService
import io.github.dudu26.moneytick.ui.theme.MoneyTickTheme

enum class Screen { DASHBOARD, SALARY, FLOAT, ABOUT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = settingsRepository
        if (repo.settings.value.notificationEnabled) FloatingWindowService.syncNotification(this, true)
        val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            FloatingWindowService.syncNotification(this, repo.settings.value.notificationEnabled)
        }
        fun setNotification(enabled: Boolean) {
            repo.update { it.copy(notificationEnabled = enabled) }
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                FloatingWindowService.syncNotification(this, enabled)
            }
        }
        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            MoneyTickTheme(dark) {
                if (!settings.onboardingDone) {
                    OnboardingScreen(settings, onFinish = { transform ->
                        repo.update(transform)
                        setNotification(repo.settings.value.notificationEnabled)
                    })
                    return@MoneyTickTheme
                }
                var screen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }
                BackHandler(enabled = screen != Screen.DASHBOARD) { screen = Screen.DASHBOARD }
                val back = { screen = Screen.DASHBOARD }
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val forward = targetState != Screen.DASHBOARD
                        (slideInHorizontally(tween(260)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(260)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(180)))
                    },
                    label = "screen",
                ) { target ->
                    when (target) {
                        Screen.DASHBOARD -> DashboardScreen(
                            settings,
                            onNavigate = { screen = it },
                            onToggleNotification = { setNotification(it) },
                        )
                        Screen.SALARY -> SalarySettingsScreen(settings, onSave = { repo.update(it); back() }, onBack = back)
                        Screen.FLOAT -> FloatSettingsScreen(settings, onChange = repo::update, onBack = back)
                        Screen.ABOUT -> AboutScreen(onBack = back)
                    }
                }
            }
        }
    }
}
