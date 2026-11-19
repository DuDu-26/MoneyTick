// SPDX-FileCopyrightText: 2026 Mr.Baoboer (PayDance), 2026 dudu (DuDu-26) (modifications)
// SPDX-License-Identifier: AGPL-3.0-only
//
// Additional terms: see /legal/ADDITIONAL_TERMS.md
// Wording adapted from PayDance src/i18n/locales/zh-CN.ts.

package io.github.dudu26.moneytick.ui

import io.github.dudu26.moneytick.core.IssueCode
import io.github.dudu26.moneytick.core.SalarySnapshot
import io.github.dudu26.moneytick.core.SalaryStatus

fun statusText(s: SalarySnapshot): String = when (s.status) {
    SalaryStatus.INVALID_CONFIG -> "配置待修正"
    SalaryStatus.REST_DAY -> "今日休息"
    SalaryStatus.BEFORE_WORK -> "未到上班"
    SalaryStatus.WORKING -> if (s.isNightWork) "正在夜班" else "正在上班"
    SalaryStatus.LUNCH_BREAK -> "午休中"
    SalaryStatus.AFTER_WORK -> if (s.isNightWork) "夜班已完成" else "已下班"
}

/** Label and value for the "time until next thing" stat, or null when nothing is pending. */
fun countdownLabel(s: SalarySnapshot): String? = when (s.status) {
    SalaryStatus.BEFORE_WORK -> "距离上班"
    SalaryStatus.LUNCH_BREAK -> "距离复工"
    SalaryStatus.WORKING -> "距离下班"
    SalaryStatus.AFTER_WORK -> "今日完成"
    else -> null
}

fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

fun issueText(code: IssueCode): String = when (code) {
    IssueCode.MONTHLY_POSITIVE -> "月薪需大于 0"
    IssueCode.DAILY_POSITIVE -> "日薪需大于 0"
    IssueCode.HOURLY_POSITIVE -> "时薪需大于 0"
    IssueCode.WORK_DAYS_POSITIVE -> "每月工作天数需大于 0"
    IssueCode.WORK_DAYS_RANGE -> "每月工作天数不能超过 31"
    IssueCode.WORKDAYS_MIN_ONE -> "至少选择一个工作日"
    IssueCode.WORKDAYS_ERROR -> "工作日数据有误"
    IssueCode.START_TIME_ERROR -> "上班时间格式应为 HH:mm"
    IssueCode.END_TIME_ERROR -> "下班时间格式应为 HH:mm"
    IssueCode.TIME_SAME -> "上下班时间不能相同"
    IssueCode.LUNCH_START_ERROR -> "午休开始时间格式应为 HH:mm"
    IssueCode.LUNCH_END_ERROR -> "午休结束时间格式应为 HH:mm"
    IssueCode.LUNCH_SAME -> "午休开始和结束不能相同"
    IssueCode.LUNCH_OUTSIDE -> "午休需在上下班时间之间"
    IssueCode.NIGHT_LUNCH_OUTSIDE -> "夜班的休息时间需在上下班时间之间"
}
