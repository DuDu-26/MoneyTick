// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class PaydayInfo(
    /** 0 on payday itself. */
    val daysUntil: Long,
    val nextPayday: LocalDate,
    /** First day of the current pay period (the day after the previous payday). */
    val periodStart: LocalDate,
    /** Earned since [periodStart], including what has been earned today so far. */
    val periodEarned: Double,
) {
    val isToday: Boolean get() = daysUntil == 0L
}

/** Payday in [month]; a day past the month's end (e.g. 31 in February) falls on the last day. */
fun paydayIn(month: YearMonth, dayOfMonth: Int): LocalDate =
    month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))

fun nextPayday(today: LocalDate, dayOfMonth: Int): LocalDate {
    val thisMonth = paydayIn(YearMonth.from(today), dayOfMonth)
    return if (!today.isAfter(thisMonth)) thisMonth else paydayIn(YearMonth.from(today).plusMonths(1), dayOfMonth)
}

/** The most recent payday strictly before [today]. */
fun previousPayday(today: LocalDate, dayOfMonth: Int): LocalDate {
    val thisMonth = paydayIn(YearMonth.from(today), dayOfMonth)
    return if (today.isAfter(thisMonth)) thisMonth else paydayIn(YearMonth.from(today).minusMonths(1), dayOfMonth)
}

/** Full pay for the shift that starts on [day], or 0 if [day] is not a workday. */
fun fullDayPay(day: LocalDate, config: SalaryConfig): Double {
    val spans = createWorkSpans(day, config)
    if (spans.isEmpty()) return 0.0
    val hours = spans.sumOf { (s, e) -> ChronoUnit.MILLIS.between(s, e) } / 3_600_000.0
    return when (config.salaryType) {
        SalaryType.MONTHLY -> config.monthlySalary / config.workDaysPerMonth
        SalaryType.DAILY -> config.dailySalary
        SalaryType.HOURLY -> config.hourlyRate * hours
    }
}

/** Returns null when no payday is configured ([dayOfMonth] = 0) or the config is invalid. */
fun calculatePayday(now: LocalDateTime, config: SalaryConfig, dayOfMonth: Int): PaydayInfo? {
    if (dayOfMonth !in 1..31 || validateSalaryConfig(config).isNotEmpty()) return null
    val today = now.toLocalDate()
    val next = nextPayday(today, dayOfMonth)
    val periodStart = previousPayday(today, dayOfMonth).plusDays(1)
    // Days whose shift is already complete; the shift in progress (which may have started
    // yesterday, for night work) is counted through today's snapshot instead.
    val currentShiftDay = resolveWorkSpans(now, config).firstOrNull()?.first?.toLocalDate() ?: today
    var earned = 0.0
    var day = periodStart
    while (day.isBefore(currentShiftDay)) {
        earned += fullDayPay(day, config)
        day = day.plusDays(1)
    }
    earned += calculateSalarySnapshot(now, config).earnedToday
    return PaydayInfo(
        daysUntil = ChronoUnit.DAYS.between(today, next),
        nextPayday = next,
        periodStart = periodStart,
        periodEarned = earned,
    )
}
