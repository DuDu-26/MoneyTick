// SPDX-FileCopyrightText: 2026 Mr.Baoboer (PayDance), 2026 dudu (DuDu-26) (Kotlin port and modifications)
// SPDX-License-Identifier: AGPL-3.0-only
//
// Additional terms: see /legal/ADDITIONAL_TERMS.md
// Ported from PayDance src/lib/salary (commit e922603). Modified for Android.

package io.github.dudu26.moneytick.core

import java.time.LocalDate
import java.time.LocalDateTime

internal const val MINUTES_PER_DAY = 24 * 60

private val timeRegex = Regex("""^(\d{1,2}):(\d{2})$""")

/** Parses "HH:mm" to minutes after midnight, or null when invalid. */
fun parseTimeToMinutes(time: String): Int? {
    val match = timeRegex.matchEntire(time) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

internal fun normalizeWorkEnd(start: Int, end: Int) = if (end < start) end + MINUTES_PER_DAY else end

internal fun normalizeTimeInsideWorkWindow(value: Int, workStart: Int) =
    if (value < workStart) value + MINUTES_PER_DAY else value

internal fun normalizeBreakEnd(breakStart: Int, breakEnd: Int): Int {
    var normalized = breakEnd
    while (normalized <= breakStart) normalized += MINUTES_PER_DAY
    return normalized
}

/** Minutes may exceed one day for shifts that cross midnight. */
internal fun dateAtMinutes(day: LocalDate, minutes: Int): LocalDateTime =
    day.atStartOfDay().plusMinutes(minutes.toLong())

/** PayDance weekday convention: 0 = Sunday … 6 = Saturday. */
internal fun LocalDate.payDanceWeekday(): Int = dayOfWeek.value % 7
