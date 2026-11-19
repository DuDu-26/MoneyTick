// SPDX-FileCopyrightText: 2026 Mr.Baoboer (PayDance), 2026 dudu (DuDu-26) (Kotlin port and modifications)
// SPDX-License-Identifier: AGPL-3.0-only
//
// Additional terms: see /legal/ADDITIONAL_TERMS.md
// Ported from PayDance src/lib/salary (commit e922603). Modified for Android.

package io.github.dudu26.moneytick.core

import kotlinx.serialization.Serializable

@Serializable
enum class SalaryType { MONTHLY, DAILY, HOURLY }

enum class SalaryStatus { INVALID_CONFIG, REST_DAY, BEFORE_WORK, WORKING, LUNCH_BREAK, AFTER_WORK }

/**
 * Salary and work-schedule settings. Times are "HH:mm" strings.
 * [workdays] uses 0 = Sunday … 6 = Saturday, the same convention as PayDance.
 */
@Serializable
data class SalaryConfig(
    val salaryType: SalaryType = SalaryType.MONTHLY,
    val monthlySalary: Double = 10000.0,
    val dailySalary: Double = 360.0,
    val hourlyRate: Double = 45.0,
    val workDaysPerMonth: Double = 22.0,
    val workdays: List<Int> = listOf(1, 2, 3, 4, 5),
    val startTime: String = "09:30",
    val endTime: String = "18:30",
    val lunchStart: String = "12:00",
    val lunchEnd: String = "13:30",
    val enableLunchBreak: Boolean = false,
)

data class SalarySnapshot(
    val earnedToday: Double = 0.0,
    val dailySalary: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val minuteRate: Double = 0.0,
    val secondRate: Double = 0.0,
    val progress: Double = 0.0,
    val isWorking: Boolean = false,
    val status: SalaryStatus = SalaryStatus.INVALID_CONFIG,
    val workMsToday: Long = 0,
    val elapsedWorkMs: Long = 0,
    val nextTransitionMs: Long = 0,
    val isNightWork: Boolean = false,
) {
    /** Remaining work time in the current shift, in milliseconds. */
    val remainingWorkMs: Long get() = (workMsToday - elapsedWorkMs).coerceAtLeast(0)
}

enum class IssueField { SALARY_TYPE, MONTHLY_SALARY, DAILY_SALARY, HOURLY_RATE, WORK_DAYS_PER_MONTH, WORKDAYS, START_TIME, END_TIME, LUNCH_START, LUNCH_END, WORK_TIME }

enum class IssueCode {
    MONTHLY_POSITIVE, DAILY_POSITIVE, HOURLY_POSITIVE, WORK_DAYS_POSITIVE, WORK_DAYS_RANGE,
    WORKDAYS_MIN_ONE, WORKDAYS_ERROR, START_TIME_ERROR, END_TIME_ERROR, TIME_SAME,
    LUNCH_START_ERROR, LUNCH_END_ERROR, LUNCH_SAME, LUNCH_OUTSIDE, NIGHT_LUNCH_OUTSIDE,
}

/** A validation problem. The UI maps [code] to localized text. */
data class SalaryConfigIssue(val field: IssueField, val code: IssueCode)
