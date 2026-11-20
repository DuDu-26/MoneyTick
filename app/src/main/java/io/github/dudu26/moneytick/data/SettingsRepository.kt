// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.data

import android.content.Context
import io.github.dudu26.moneytick.MoneyTickApp
import io.github.dudu26.moneytick.core.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for settings, shared by the activity and the floating window
 * (same process). Stored locally only, as JSON in SharedPreferences. No network, no account.
 */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("moneytick", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        prefs.getString(KEY, null).let { raw ->
            // Users who saved settings before the onboarding existed have already set up.
            AppSettings.decode(raw).let { if (raw != null && !it.onboardingDone) it.copy(onboardingDone = true) else it }
        },
    )
    val settings: StateFlow<AppSettings> = state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        state.update(transform)
        prefs.edit().putString(KEY, AppSettings.encode(state.value)).apply()
    }

    companion object {
        private const val KEY = "app_settings"
    }
}

val Context.settingsRepository: SettingsRepository
    get() = (applicationContext as MoneyTickApp).settings
