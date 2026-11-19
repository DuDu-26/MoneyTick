// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dudu26.moneytick.core.AmountAnimation
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.SalaryType
import io.github.dudu26.moneytick.core.ThemeMode
import io.github.dudu26.moneytick.core.sanitizeCurrencySymbol
import io.github.dudu26.moneytick.core.validateSalaryConfig

private val weekdayLabels = listOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 0 to "日")

private fun Double.plain() = if (this % 1.0 == 0.0) toLong().toString() else toString()

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

    val draft = s.copy(
        salaryType = type,
        monthlySalary = monthly.toDoubleOrNull() ?: Double.NaN,
        dailySalary = daily.toDoubleOrNull() ?: Double.NaN,
        hourlyRate = hourly.toDoubleOrNull() ?: Double.NaN,
        workDaysPerMonth = daysPerMonth.toDoubleOrNull() ?: Double.NaN,
        workdays = workdays.sorted(),
        startTime = start.trim(), endTime = end.trim(),
        enableLunchBreak = lunch, lunchStart = lunchStart.trim(), lunchEnd = lunchEnd.trim(),
    )
    val issues = validateSalaryConfig(draft)

    SubScreen("薪资设置", onBack) {
        SectionTitle("薪资模式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SalaryType.MONTHLY to "月薪", SalaryType.DAILY to "日薪", SalaryType.HOURLY to "时薪").forEach { (t, label) ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(label) })
            }
        }
        SectionTitle("薪资")
        when (type) {
            SalaryType.MONTHLY -> {
                NumberField("月薪（元）", monthly) { monthly = it }
                NumberField("每月工作天数", daysPerMonth) { daysPerMonth = it }
            }
            SalaryType.DAILY -> NumberField("日薪（元）", daily) { daily = it }
            SalaryType.HOURLY -> NumberField("时薪（元）", hourly) { hourly = it }
        }
        SectionTitle("每周工作日")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekdayLabels.forEach { (day, label) ->
                FilterChip(
                    selected = day in workdays,
                    onClick = { workdays = if (day in workdays) workdays - day else workdays + day },
                    label = { Text(label) },
                )
            }
        }
        SectionTitle("工作时间（HH:mm，下班早于上班即为跨零点夜班）")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField("上班", start, Modifier.weight(1f)) { start = it }
            TimeField("下班", end, Modifier.weight(1f)) { end = it }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("剔除午休", Modifier.weight(1f))
            Switch(checked = lunch, onCheckedChange = { lunch = it })
        }
        if (lunch) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("午休开始", lunchStart, Modifier.weight(1f)) { lunchStart = it }
                TimeField("午休结束", lunchEnd, Modifier.weight(1f)) { lunchEnd = it }
            }
        }
        SectionTitle("显示")
        OutlinedTextField(
            value = currency, onValueChange = { currency = sanitizeCurrencySymbol(it) },
            label = { Text("货币符号（留空不显示，最多 4 个字符）") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(AmountAnimation.ROLLING to "滚动变换", AmountAnimation.PLAIN to "直接变换").forEach { (a, label) ->
                FilterChip(selected = animation == a, onClick = { animation = a }, label = { Text(label) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ThemeMode.SYSTEM to "跟随系统", ThemeMode.LIGHT to "浅色", ThemeMode.DARK to "深色").forEach { (m, label) ->
                FilterChip(selected = theme == m, onClick = { theme = m }, label = { Text(label) })
            }
        }

        issues.forEach {
            Text(issueText(it.code), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            enabled = issues.isEmpty(),
            onClick = {
                onSave { it.copy(salary = draft, currencySymbol = currency, amountAnimation = animation, theme = theme) }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) { Text("保存") }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(
    value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
)

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) = OutlinedTextField(
    value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
)
