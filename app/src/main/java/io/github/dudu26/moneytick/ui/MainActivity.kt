// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dudu26.moneytick.core.ThemeMode
import io.github.dudu26.moneytick.data.settingsRepository

enum class Screen { DASHBOARD, SALARY, FLOAT, ABOUT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = settingsRepository
        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                var screen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }
                BackHandler(enabled = screen != Screen.DASHBOARD) { screen = Screen.DASHBOARD }
                val back = { screen = Screen.DASHBOARD }
                when (screen) {
                    Screen.DASHBOARD -> DashboardScreen(settings, onNavigate = { screen = it })
                    Screen.SALARY -> SalarySettingsScreen(settings, onSave = { repo.update(it); back() }, onBack = back)
                    Screen.FLOAT -> FloatSettingsScreen(settings, onChange = repo::update, onBack = back)
                    Screen.ABOUT -> AboutScreen(onBack = back)
                }
            }
        }
    }
}
