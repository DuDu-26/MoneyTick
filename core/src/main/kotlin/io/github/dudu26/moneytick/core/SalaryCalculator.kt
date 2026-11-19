// SPDX-FileCopyrightText: 2026 Mr.Baoboer (PayDance), 2026 dudu (DuDu-26) (Kotlin port and modifications)
// SPDX-License-Identifier: AGPL-3.0-only
//
// Additional terms: see /legal/ADDITIONAL_TERMS.md
// Ported from PayDance src/lib/salary (commit e922603). Modified for Android.

package io.github.dudu26.moneytick.core

import java.time.Duration
import java.time.LocalDateTime

private const val NIGHT_START_HOUR = 22
private const val NIGHT_END_HOUR = 6

private fun msBetween(a: LocalDateTime, b: LocalDateTime) = Duration.between(a, b).toMillis()

fun isWithinNightWorkWindow(now: LocalDateTime): Boolean =
    now.hour >= NIGHT_START_HOUR || now.hour < NIGHT_END_HOUR

fun spansTouchNightWorkWindow(spans: List<WorkSpan>): Boolean {
    for ((spanStart, spanEnd) in spans) {
        val firstDay = spanStart.toLocalDate().minusDays(1)
        for (offset in 0L until 4L) {
            val day = firstDay.plusDays(offset)
            val windowStart = day.atTime(NIGHT_START_HOUR, 0)
            val windowEnd = day.plusDays(1).atTime(NIGHT_END_HOUR, 0)
            if (spanStart.isBefore(windowEnd) && spanEnd.isAfter(windowStart)) return true
        }
    }
    return false
}

private fun dailySalaryOf(config: SalaryConfig, totalWorkMs: Long): Double = when (config.salaryType) {
    SalaryType.DAILY -> config.dailySalary
    SalaryType.HOURLY -> config.hourlyRate * (totalWorkMs / 3_600_000.0)
    SalaryType.MONTHLY -> config.monthlySalary / config.workDaysPerMonth
}

/** Computes how much has been earned at [now]. Pure function; call it on every tick. */
fun calculateSalarySnapshot(now: LocalDateTime, config: SalaryConfig): SalarySnapshot {
    if (validateSalaryConfig(config).isNotEmpty()) return SalarySnapshot(status = SalaryStatus.INVALID_CONFIG)
    val spans = resolveWorkSpans(now, config)
    if (spans.isEmpty()) return SalarySnapshot(status = SalaryStatus.REST_DAY)

    val totalWorkMs = spans.sumOf { (s, e) -> msBetween(s, e).coerceAtLeast(0) }
    if (totalWorkMs <= 0) return SalarySnapshot(status = SalaryStatus.INVALID_CONFIG)

    val elapsedWorkMs = spans.sumOf { (s, e) -> msBetween(s, now).coerceIn(0, msBetween(s, e)) }
    val daily = dailySalaryOf(config, totalWorkMs)
    val hourly = daily / (totalWorkMs / 3_600_000.0)
    val progress = (elapsedWorkMs.toDouble() / totalWorkMs).coerceIn(0.0, 1.0)

    val status = when {
        spans.any { (s, e) -> !now.isBefore(s) && now.isBefore(e) } -> SalaryStatus.WORKING
        elapsedWorkMs <= 0 -> SalaryStatus.BEFORE_WORK
        elapsedWorkMs >= totalWorkMs -> SalaryStatus.AFTER_WORK
        else -> SalaryStatus.LUNCH_BREAK
    }
    val nextTransitionMs = when (status) {
        SalaryStatus.BEFORE_WORK -> msBetween(now, spans.first().first).coerceAtLeast(0)
        SalaryStatus.LUNCH_BREAK -> spans.firstOrNull { it.first.isAfter(now) }
            ?.let { msBetween(now, it.first).coerceAtLeast(0) } ?: 0
        SalaryStatus.WORKING -> msBetween(now, spans.last().second).coerceAtLeast(0)
        else -> 0
    }
    val isNight = (status == SalaryStatus.WORKING && isWithinNightWorkWindow(now)) ||
        (status == SalaryStatus.AFTER_WORK && spansTouchNightWorkWindow(spans))

    return SalarySnapshot(
        earnedToday = daily * progress,
        dailySalary = daily,
        hourlyRate = hourly,
        minuteRate = hourly / 60,
        secondRate = hourly / 3600,
        progress = progress,
        isWorking = status == SalaryStatus.WORKING,
        status = status,
        workMsToday = totalWorkMs,
        elapsedWorkMs = elapsedWorkMs,
        nextTransitionMs = nextTransitionMs,
        isNightWork = isNight,
    )
}

/**
 * Delay until the next UI refresh: fast enough to show every cent while working,
 * 1 s otherwise. Mirrors PayDance useSalaryTicker.
 */
fun nextTickDelayMs(snapshot: SalarySnapshot): Long {
    if (snapshot.status != SalaryStatus.WORKING || snapshot.secondRate <= 0) {
        return if (snapshot.nextTransitionMs > 0) snapshot.nextTransitionMs.coerceIn(120, 1000) else 1000
    }
    return ((0.01 / snapshot.secondRate) * 1000).toLong().coerceIn(120, 1000)
}
