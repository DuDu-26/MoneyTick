// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dudu26.moneytick.core.AmountAnimation
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.RefreshRate
import io.github.dudu26.moneytick.core.SalaryType
import io.github.dudu26.moneytick.core.ThemeMode
import io.github.dudu26.moneytick.core.isOvernightWorkConfig
import io.github.dudu26.moneytick.core.parseTimeToMinutes
import io.github.dudu26.moneytick.core.sanitizeCurrencySymbol
import io.github.dudu26.moneytick.core.validateSalaryConfig
import io.github.dudu26.moneytick.ui.components.GroupCard
import io.github.dudu26.moneytick.ui.components.LargeTitlePage
import io.github.dudu26.moneytick.ui.components.RowDivider
import io.github.dudu26.moneytick.ui.components.SectionHeader
import io.github.dudu26.moneytick.ui.components.SegmentedPicker
import io.github.dudu26.moneytick.ui.components.SettingRow
import io.github.dudu26.moneytick.ui.theme.extraColors

internal val weekdayLabels = listOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 0 to "日")

private fun Double.plain() = if (this % 1.0 == 0.0) toLong().toString() else toString()

private enum class TimeTarget(val title: String) { START("上班时间"), END("下班时间"), LUNCH_START("午休开始"), LUNCH_END("午休结束") }

@Composable
fun SalarySettingsScreen(settings: AppSettings, onSave: ((AppSettings) -> AppSettings) -> Unit, onBack: () -> Unit) {
    val s = settings.salary
    var type by remember { mutableStateOf(s.salaryType) }
    var monthly by remember { mutableStateOf(s.monthlySalary.plain()) }
    var daily by remember { mutableStateOf(s.dailySalary.plain()) }
    var hourly by remember { mutableStateOf(s.hourlyRate.plain()) }
    var daysPerMonth by remember { mutableStateOf(s.workDaysPerMonth.plain()) }
    var workdays by remember { mutableStateOf(s.workdays.toSet()) }
    var start by remember { mutableStateOf(s.startTime) }
    var end by remember { mutableStateOf(s.endTime) }
    var lunch by remember { mutableStateOf(s.enableLunchBreak) }
    var lunchStart by remember { mutableStateOf(s.lunchStart) }
    var lunchEnd by remember { mutableStateOf(s.lunchEnd) }
    var currency by remember { mutableStateOf(settings.currencySymbol) }
    var animation by remember { mutableStateOf(settings.amountAnimation) }
    var theme by remember { mutableStateOf(settings.theme) }
    var refresh by remember { mutableStateOf(settings.refreshRate) }
    var editing by remember { mutableStateOf<TimeTarget?>(null) }
    var payday by remember { mutableStateOf(settings.paydayOfMonth) }
    var pickingPayday by remember { mutableStateOf(false) }

    val draft = s.copy(
        salaryType = type,
        monthlySalary = monthly.toDoubleOrNull() ?: Double.NaN,
        dailySalary = daily.toDoubleOrNull() ?: Double.NaN,
        hourlyRate = hourly.toDoubleOrNull() ?: Double.NaN,
        workDaysPerMonth = daysPerMonth.toDoubleOrNull() ?: Double.NaN,
        workdays = workdays.sorted(),
        startTime = start, endTime = end,
        enableLunchBreak = lunch, lunchStart = lunchStart, lunchEnd = lunchEnd,
    )
    val issues = validateSalaryConfig(draft)

    LargeTitlePage(
        title = "薪资与作息",
        onBack = onBack,
        bottomBar = {
            Button(
                enabled = issues.isEmpty(),
                onClick = {
                    onSave {
                        it.copy(
                            salary = draft, currencySymbol = currency, amountAnimation = animation,
                            theme = theme, refreshRate = refresh, paydayOfMonth = payday,
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("保存", style = MaterialTheme.typography.titleMedium) }
        },
    ) {
        SectionHeader("薪资")
        GroupCard {
            Column(Modifier.padding(12.dp)) {
                SegmentedPicker(
                    listOf(SalaryType.MONTHLY to "月薪", SalaryType.DAILY to "日薪", SalaryType.HOURLY to "时薪"),
                    type, { type = it },
                )
            }
            when (type) {
                SalaryType.MONTHLY -> {
                    AmountRow("月薪", monthly, "元") { monthly = it }
                    RowDivider()
                    AmountRow("每月工作天数", daysPerMonth, "天") { daysPerMonth = it }
                }
                SalaryType.DAILY -> AmountRow("日薪", daily, "元") { daily = it }
                SalaryType.HOURLY -> AmountRow("时薪", hourly, "元/时") { hourly = it }
            }
        }

        SectionHeader("每周工作日")
        GroupCard {
            WeekdayPicker(workdays, Modifier.padding(12.dp)) { workdays = it }
        }

        SectionHeader("工作时间")
        GroupCard {
            val overnight = isOvernightWorkConfig(draft)
            SettingRow("上班", null, Icons.Rounded.DateRange, onClick = { editing = TimeTarget.START }, trailing = { TimeValue(start) })
            RowDivider()
            SettingRow(
                "下班", if (overnight) "次日下班（跨零点夜班）" else null, Icons.Rounded.DateRange,
                onClick = { editing = TimeTarget.END }, trailing = { TimeValue(end) },
            )
            RowDivider()
            SettingRow("剔除午休", "午休时间不计薪", Icons.Rounded.Favorite, trailing = { Switch(checked = lunch, onCheckedChange = { lunch = it }) })
            if (lunch) {
                RowDivider()
                SettingRow("午休开始", onClick = { editing = TimeTarget.LUNCH_START }, trailing = { TimeValue(lunchStart) })
                RowDivider()
                SettingRow("午休结束", onClick = { editing = TimeTarget.LUNCH_END }, trailing = { TimeValue(lunchEnd) })
            }
        }

        SectionHeader("发薪")
        GroupCard {
            SettingRow(
                "发薪日", "首页显示发薪倒计时和本期已挣", Icons.Rounded.Star,
                onClick = { pickingPayday = true },
                trailing = { TimeValue(if (payday == 0) "未设置" else "每月 $payday 日") },
            )
        }

        if (issues.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error)
                Column(Modifier.padding(start = 10.dp)) {
                    issues.map { issueText(it.code) }.distinct().forEach {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        SectionHeader("显示")
        GroupCard {
            SettingRow("货币符号", "留空不显示，最多 4 个字符", trailing = {
                InlineField(currency, KeyboardType.Text, Modifier.width(72.dp)) { currency = sanitizeCurrencySymbol(it) }
            })
            RowDivider()
            Column(Modifier.padding(12.dp)) {
                Text("金额变换", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                SegmentedPicker(listOf(AmountAnimation.ROLLING to "滚动", AmountAnimation.PLAIN to "直接"), animation, { animation = it })
                Text("外观", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                SegmentedPicker(
                    listOf(ThemeMode.SYSTEM to "跟随系统", ThemeMode.LIGHT to "浅色", ThemeMode.DARK to "深色"),
                    theme, { theme = it },
                )
            }
        }

        SectionHeader("省电")
        GroupCard {
            SettingRow("刷新频率", "越慢越省电；下班和休息日自动降到每分钟一次", Icons.Rounded.Refresh)
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                SegmentedPicker(
                    listOf(
                        RefreshRate.REALTIME to "实时", RefreshRate.SECOND to "1 秒",
                        RefreshRate.FIVE_SECONDS to "5 秒", RefreshRate.MINUTE to "1 分钟",
                    ),
                    refresh, { refresh = it },
                )
            }
        }
    }

    if (pickingPayday) {
        PaydayDialog(payday, onDismiss = { pickingPayday = false }) { payday = it; pickingPayday = false }
    }

    editing?.let { target ->
        val current = when (target) {
            TimeTarget.START -> start
            TimeTarget.END -> end
            TimeTarget.LUNCH_START -> lunchStart
            TimeTarget.LUNCH_END -> lunchEnd
        }
        TimePickerDialog(target.title, current, onDismiss = { editing = null }) { value ->
            when (target) {
                TimeTarget.START -> start = value
                TimeTarget.END -> end = value
                TimeTarget.LUNCH_START -> lunchStart = value
                TimeTarget.LUNCH_END -> lunchEnd = value
            }
            editing = null
        }
    }
}

@Composable
internal fun TimeValue(value: String) = Text(
    value,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(extraColors.subtle).padding(horizontal = 12.dp, vertical = 6.dp),
)

@Composable
private fun AmountRow(label: String, value: String, unit: String, onChange: (String) -> Unit) {
    SettingRow(label, trailing = {
        InlineField(value, KeyboardType.Decimal, Modifier.width(120.dp)) { input ->
            onChange(input.filter { it.isDigit() || it == '.' })
        }
        Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    })
}

@Composable
internal fun InlineField(value: String, keyboard: KeyboardType, modifier: Modifier, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(extraColors.subtle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val minutes = parseTimeToMinutes(initial) ?: (9 * 60)
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
internal fun WeekdayPicker(selected: Set<Int>, modifier: Modifier = Modifier, onChange: (Set<Int>) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        weekdayLabels.forEach { (day, label) ->
            val on = day in selected
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else extraColors.subtle)
                    .clickable { onChange(if (on) selected - day else selected + day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Day-of-month chooser for payday: 1–31 grid plus "不设置". */
@Composable
internal fun PaydayDialog(current: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每月几号发工资？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..31).chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { d ->
                            val on = d == current
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (on) MaterialTheme.colorScheme.primary else extraColors.subtle)
                                    .clickable { onPick(d) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$d", color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                    }
                }
                Text(
                    "遇到小月会自动算到月底那天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        dismissButton = { TextButton(onClick = { onPick(0) }) { Text("不设置") } },
    )
}
