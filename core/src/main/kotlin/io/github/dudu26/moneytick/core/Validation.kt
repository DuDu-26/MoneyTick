// SPDX-FileCopyrightText: 2026 Mr.Baoboer (PayDance), 2026 dudu (DuDu-26) (Kotlin port and modifications)
// SPDX-License-Identifier: AGPL-3.0-only
//
// Additional terms: see /legal/ADDITIONAL_TERMS.md
// Ported from PayDance src/lib/salary (commit e922603). Modified for Android.

package io.github.dudu26.moneytick.core

const val MAX_WORK_DAYS_PER_MONTH = 31

private fun Double.isPositive() = isFinite() && this > 0

fun isOvernightWorkConfig(config: SalaryConfig): Boolean {
    val start = parseTimeToMinutes(config.startTime) ?: return false
    val end = parseTimeToMinutes(config.endTime) ?: return false
    return end < start
}

fun validateSalaryConfig(config: SalaryConfig): List<SalaryConfigIssue> {
    val issues = mutableListOf<SalaryConfigIssue>()
    fun add(field: IssueField, code: IssueCode) = issues.add(SalaryConfigIssue(field, code))
    val start = parseTimeToMinutes(config.startTime)
    val end = parseTimeToMinutes(config.endTime)
    val type = config.salaryType

    if (type == SalaryType.MONTHLY && !config.monthlySalary.isPositive()) add(IssueField.MONTHLY_SALARY, IssueCode.MONTHLY_POSITIVE)
    if (type == SalaryType.DAILY && !config.dailySalary.isPositive()) add(IssueField.DAILY_SALARY, IssueCode.DAILY_POSITIVE)
    if (type == SalaryType.HOURLY && !config.hourlyRate.isPositive()) add(IssueField.HOURLY_RATE, IssueCode.HOURLY_POSITIVE)
    if (type == SalaryType.MONTHLY && !config.workDaysPerMonth.isPositive()) {
        add(IssueField.WORK_DAYS_PER_MONTH, IssueCode.WORK_DAYS_POSITIVE)
    } else if (type == SalaryType.MONTHLY && config.workDaysPerMonth > MAX_WORK_DAYS_PER_MONTH) {
        add(IssueField.WORK_DAYS_PER_MONTH, IssueCode.WORK_DAYS_RANGE)
    }
    if (config.workdays.isEmpty()) {
        add(IssueField.WORKDAYS, IssueCode.WORKDAYS_MIN_ONE)
    } else if (!config.workdays.all { it in 0..6 }) {
        add(IssueField.WORKDAYS, IssueCode.WORKDAYS_ERROR)
    }
    if (start == null) add(IssueField.START_TIME, IssueCode.START_TIME_ERROR)
    if (end == null) add(IssueField.END_TIME, IssueCode.END_TIME_ERROR)
    if (start != null && end != null && start == end) add(IssueField.WORK_TIME, IssueCode.TIME_SAME)

    if (!config.enableLunchBreak) return issues

    val lunchStart = parseTimeToMinutes(config.lunchStart)
    val lunchEnd = parseTimeToMinutes(config.lunchEnd)
    if (lunchStart == null) add(IssueField.LUNCH_START, IssueCode.LUNCH_START_ERROR)
    if (lunchEnd == null) add(IssueField.LUNCH_END, IssueCode.LUNCH_END_ERROR)
    if (lunchStart != null && lunchEnd != null && lunchStart == lunchEnd) {
        add(IssueField.WORK_TIME, IssueCode.LUNCH_SAME)
        return issues
    }
    if (start != null && end != null && start != end && lunchStart != null && lunchEnd != null) {
        val workEnd = normalizeWorkEnd(start, end)
        val nLunchStart = normalizeTimeInsideWorkWindow(lunchStart, start)
        val nLunchEnd = normalizeBreakEnd(nLunchStart, lunchEnd)
        if (!(start < nLunchStart && nLunchStart < nLunchEnd && nLunchEnd < workEnd)) {
            add(IssueField.WORK_TIME, if (end < start) IssueCode.NIGHT_LUNCH_OUTSIDE else IssueCode.LUNCH_OUTSIDE)
        }
    }
    return issues
}
