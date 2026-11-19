// SPDX-FileCopyrightText: 2026 Mr.Baoboer (PayDance), 2026 dudu (DuDu-26) (Kotlin port and modifications)
// SPDX-License-Identifier: AGPL-3.0-only
//
// Additional terms: see /legal/ADDITIONAL_TERMS.md
// Ported from PayDance src/lib/salary (commit e922603). Modified for Android.

package io.github.dudu26.moneytick.core

import java.time.LocalDate
import java.time.LocalDateTime

typealias WorkSpan = Pair<LocalDateTime, LocalDateTime>

fun isConfiguredWorkday(day: LocalDate, config: SalaryConfig): Boolean =
    config.workdays.isNotEmpty() && day.payDanceWeekday() in config.workdays

fun createWorkSpans(day: LocalDate, config: SalaryConfig): List<WorkSpan> {
    if (!isConfiguredWorkday(day, config)) return emptyList()
    val start = parseTimeToMinutes(config.startTime) ?: return emptyList()
    val end = parseTimeToMinutes(config.endTime) ?: return emptyList()
    if (start == end) return emptyList()
    val workEnd = normalizeWorkEnd(start, end)
    if (!config.enableLunchBreak) {
        return listOf(dateAtMinutes(day, start) to dateAtMinutes(day, workEnd))
    }
    val lunchStart = parseTimeToMinutes(config.lunchStart) ?: return emptyList()
    val lunchEnd = parseTimeToMinutes(config.lunchEnd) ?: return emptyList()
    val nLunchStart = normalizeTimeInsideWorkWindow(lunchStart, start)
    val nLunchEnd = normalizeBreakEnd(nLunchStart, lunchEnd)
    if (start < nLunchStart && nLunchStart < nLunchEnd && nLunchEnd < workEnd) {
        return listOf(
            dateAtMinutes(day, start) to dateAtMinutes(day, nLunchStart),
            dateAtMinutes(day, nLunchEnd) to dateAtMinutes(day, workEnd),
        )
    }
    return emptyList()
}

private fun isWithinSpanBounds(now: LocalDateTime, spans: List<WorkSpan>): Boolean =
    spans.isNotEmpty() && !now.isBefore(spans.first().first) && !now.isAfter(spans.last().second)

private fun didSpanEndToday(now: LocalDateTime, spans: List<WorkSpan>): Boolean {
    if (spans.isEmpty()) return false
    val lastEnd = spans.last().second
    return now.isAfter(lastEnd) && now.toLocalDate().isEqual(lastEnd.toLocalDate())
}

private fun hasNotReachedFirstSpan(now: LocalDateTime, spans: List<WorkSpan>) =
    spans.isNotEmpty() && now.isBefore(spans.first().first)

/** Picks today's spans, or yesterday's when an overnight shift is still running or just ended. */
fun resolveWorkSpans(now: LocalDateTime, config: SalaryConfig): List<WorkSpan> {
    val previous = createWorkSpans(now.toLocalDate().minusDays(1), config)
    if (isWithinSpanBounds(now, previous)) return previous
    val current = createWorkSpans(now.toLocalDate(), config)
    if (didSpanEndToday(now, previous) && (current.isEmpty() || hasNotReachedFirstSpan(now, current))) {
        return previous
    }
    return current
}
