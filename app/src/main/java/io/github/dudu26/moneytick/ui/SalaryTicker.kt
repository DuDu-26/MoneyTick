// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.PaydayInfo
import io.github.dudu26.moneytick.core.calculatePayday
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.dudu26.moneytick.core.RefreshRate
import io.github.dudu26.moneytick.core.SalaryConfig
import io.github.dudu26.moneytick.core.SalarySnapshot
import io.github.dudu26.moneytick.core.calculateSalarySnapshot
import io.github.dudu26.moneytick.core.nextTickDelayMs
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Source of "now" for the UI. Screenshot tests replace it with a fixed time. */
val LocalNow = staticCompositionLocalOf<() -> LocalDateTime> { { LocalDateTime.now() } }

/**
 * Recomputes the snapshot only as often as needed (see [nextTickDelayMs]) and only while
 * something can see it: the host lifecycle is STARTED and [visible] is true (the floating
 * window passes false while the screen is off). When paused, no timer runs at all.
 */
@Composable
fun rememberSalarySnapshot(
    config: SalaryConfig,
    rate: RefreshRate = RefreshRate.REALTIME,
    visible: Boolean = true,
): SalarySnapshot {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val active = visible && lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val now = LocalNow.current
    var snapshot by remember(config) { mutableStateOf(calculateSalarySnapshot(now(), config)) }
    LaunchedEffect(config, rate, active) {
        while (active) {
            snapshot = calculateSalarySnapshot(now(), config)
            delay(nextTickDelayMs(snapshot, rate))
        }
    }
    return snapshot
}

/** Pay-period info, recomputed whenever the snapshot ticks. Null when no payday is set. */
@Composable
fun rememberPayday(settings: AppSettings, snapshot: SalarySnapshot): PaydayInfo? {
    val now = LocalNow.current
    return remember(settings.salary, settings.paydayOfMonth, snapshot) {
        calculatePayday(now(), settings.salary, settings.paydayOfMonth)
    }
}
