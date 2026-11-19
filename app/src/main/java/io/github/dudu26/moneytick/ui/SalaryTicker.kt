// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.dudu26.moneytick.core.SalaryConfig
import io.github.dudu26.moneytick.core.SalarySnapshot
import io.github.dudu26.moneytick.core.calculateSalarySnapshot
import io.github.dudu26.moneytick.core.nextTickDelayMs
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Recomputes the snapshot as often as the amount can visibly change (every cent). */
@Composable
fun rememberSalarySnapshot(config: SalaryConfig): SalarySnapshot {
    var snapshot by remember(config) { mutableStateOf(calculateSalarySnapshot(LocalDateTime.now(), config)) }
    LaunchedEffect(config) {
        while (true) {
            snapshot = calculateSalarySnapshot(LocalDateTime.now(), config)
            delay(nextTickDelayMs(snapshot))
        }
    }
    return snapshot
}
