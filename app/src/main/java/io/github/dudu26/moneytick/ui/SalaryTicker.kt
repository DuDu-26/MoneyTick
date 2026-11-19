// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var snapshot by remember(config) { mutableStateOf(calculateSalarySnapshot(LocalDateTime.now(), config)) }
    LaunchedEffect(config, rate, active) {
        while (active) {
            snapshot = calculateSalarySnapshot(LocalDateTime.now(), config)
            delay(nextTickDelayMs(snapshot, rate))
        }
    }
    return snapshot
}
