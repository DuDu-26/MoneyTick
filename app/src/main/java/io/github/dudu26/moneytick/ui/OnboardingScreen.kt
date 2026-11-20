// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.SalaryType
import io.github.dudu26.moneytick.core.formatAmount
import io.github.dudu26.moneytick.core.isOvernightWorkConfig
import io.github.dudu26.moneytick.core.validateSalaryConfig
import io.github.dudu26.moneytick.ui.components.SegmentedPicker
import io.github.dudu26.moneytick.ui.theme.NumberStyle
import io.github.dudu26.moneytick.ui.theme.extraColors
import io.github.dudu26.moneytick.ui.theme.softCard

private fun Double.plain() = if (this % 1.0 == 0.0) toLong().toString() else toString()

/** Three-step first-run setup: pay → hours → payday & extras. */
@Composable
fun OnboardingScreen(settings: AppSettings, onFinish: ((AppSettings) -> AppSettings) -> Unit, initialStep: Int = 0) {
    val s = settings.salary
    var step by rememberSaveable { mutableIntStateOf(initialStep) }
    var type by rememberSaveable { mutableStateOf(s.salaryType) }
    var amount by rememberSaveable {
        mutableStateOf(
            when (s.salaryType) {
                SalaryType.MONTHLY -> s.monthlySalary.plain()
                SalaryType.DAILY -> s.dailySalary.plain()
                SalaryType.HOURLY -> s.hourlyRate.plain()
            },
        )
    }
    var daysPerMonth by rememberSaveable { mutableStateOf(s.workDaysPerMonth.plain()) }
    var workdays by rememberSaveable { mutableStateOf(s.workdays.toSet()) }
    var start by rememberSaveable { mutableStateOf(s.startTime) }
    var end by rememberSaveable { mutableStateOf(s.endTime) }
    var lunch by rememberSaveable { mutableStateOf(s.enableLunchBreak) }
    var lunchStart by rememberSaveable { mutableStateOf(s.lunchStart) }
    var lunchEnd by rememberSaveable { mutableStateOf(s.lunchEnd) }
    var payday by rememberSaveable { mutableIntStateOf(settings.paydayOfMonth.takeIf { it > 0 } ?: 15) }
    var notify by rememberSaveable { mutableStateOf(settings.notificationEnabled) }
    var editingTime by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingPayday by rememberSaveable { mutableStateOf(false) }

    val value = amount.toDoubleOrNull() ?: Double.NaN
    val draft = s.copy(
        salaryType = type,
        monthlySalary = if (type == SalaryType.MONTHLY) value else s.monthlySalary,
        dailySalary = if (type == SalaryType.DAILY) value else s.dailySalary,
        hourlyRate = if (type == SalaryType.HOURLY) value else s.hourlyRate,
        workDaysPerMonth = daysPerMonth.toDoubleOrNull() ?: Double.NaN,
        workdays = workdays.sorted(),
        startTime = start, endTime = end, enableLunchBreak = lunch, lunchStart = lunchStart, lunchEnd = lunchEnd,
    )
    val issues = validateSalaryConfig(draft)
    val stepValid = when (step) {
        0 -> value.isFinite() && value > 0 && (type != SalaryType.MONTHLY || (draft.workDaysPerMonth in 1.0..31.0))
        1 -> issues.isEmpty()
        else -> true
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            // Progress dots + skip
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(40.dp)) {
                repeat(3) { i ->
                    val w by animateDpAsState(if (i == step) 24.dp else 8.dp, tween(220), label = "dot")
                    Box(
                        Modifier.padding(end = 6.dp).height(8.dp).width(w).clip(CircleShape)
                            .background(if (i <= step) extraColors.ink else extraColors.subtle),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (step > 0) TextButton(onClick = { step-- }) { Text("上一步", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val fwd = targetState > initialState
                    (slideInHorizontally(tween(260)) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(260)) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(160)))
                },
                label = "step",
                modifier = Modifier.weight(1f),
            ) { current ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
                    when (current) {
                        0 -> {
                            StepTitle("你的工资怎么算？", "选一种方式，填个数字就行。数据只存在你手机里。")
                            SegmentedPicker(
                                listOf(SalaryType.MONTHLY to "月薪", SalaryType.DAILY to "日薪", SalaryType.HOURLY to "时薪"),
                                type, { type = it },
                            )
                            Spacer(Modifier.height(20.dp))
                            BigAmountInput(amount, settings.currencySymbol) { amount = it }
                            if (type == SalaryType.MONTHLY) {
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 16.dp).softCard().padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("每月上几天班", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    InlineField(daysPerMonth, KeyboardType.Decimal, Modifier.width(80.dp)) { daysPerMonth = it.filter { c -> c.isDigit() || c == '.' } }
                                    Text("天", modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                        1 -> {
                            StepTitle("几点上下班？", "下班时间早于上班，就当跨零点夜班算。")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TimeTile("上班", start, Modifier.weight(1f)) { editingTime = "start" }
                                TimeTile(if (isOvernightWorkConfig(draft)) "下班（次日）" else "下班", end, Modifier.weight(1f)) { editingTime = "end" }
                            }
                            Text("每周哪几天上班", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
                            WeekdayPicker(workdays) { workdays = it }
                            Column(Modifier.fillMaxWidth().padding(top = 20.dp).softCard().padding(horizontal = 18.dp, vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("剔除午休", style = MaterialTheme.typography.bodyLarge)
                                        Text("午休不算钱（老板是这么想的）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = lunch, onCheckedChange = { lunch = it })
                                }
                                if (lunch) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                                        Box(Modifier.weight(1f).clickable { editingTime = "lunchStart" }) { TimeValue("开始 $lunchStart") }
                                        Box(Modifier.weight(1f).clickable { editingTime = "lunchEnd" }) { TimeValue("结束 $lunchEnd") }
                                    }
                                }
                            }
                            issues.map { issueText(it.code) }.distinct().forEach {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
                            }
                        }
                        else -> {
                            StepTitle("最后一步", "发薪日用来倒计时，也能算出这个月已经挣了多少。")
                            Row(
                                Modifier.fillMaxWidth().softCard().clickable { pickingPayday = true }.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("发薪日", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                TimeValue(if (payday == 0) "不设置" else "每月 $payday 日")
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(top = 14.dp).softCard().padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("通知栏显示金额", style = MaterialTheme.typography.bodyLarge)
                                    Text("下拉就能看今天挣了多少", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = notify, onCheckedChange = { notify = it })
                            }
                            Text(
                                "按你的设置，一天能挣 ${formatAmount(fullDayEstimate(draft), settings.currencySymbol)}。悬浮窗在首页底部的黑色按钮里开。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 20.dp),
                            )
                        }
                    }
                }
            }

            Button(
                enabled = stepValid,
                onClick = {
                    if (step < 2) {
                        step++
                    } else {
                        onFinish {
                            it.copy(
                                salary = draft, paydayOfMonth = payday,
                                notificationEnabled = notify, onboardingDone = true,
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(if (step < 2) "下一步" else "开始摸鱼", style = MaterialTheme.typography.titleMedium) }
        }
    }

    editingTime?.let { which ->
        val (title, cur) = when (which) {
            "start" -> "上班时间" to start
            "end" -> "下班时间" to end
            "lunchStart" -> "午休开始" to lunchStart
            else -> "午休结束" to lunchEnd
        }
        TimePickerDialog(title, cur, onDismiss = { editingTime = null }) { v ->
            when (which) {
                "start" -> start = v
                "end" -> end = v
                "lunchStart" -> lunchStart = v
                else -> lunchEnd = v
            }
            editingTime = null
        }
    }
    if (pickingPayday) {
        PaydayDialog(payday, onDismiss = { pickingPayday = false }) { payday = it; pickingPayday = false }
    }
}

private fun fullDayEstimate(c: io.github.dudu26.moneytick.core.SalaryConfig): Double {
    val today = java.time.LocalDate.now()
    // Any configured workday gives the same full-day pay; search the coming week for one.
    return (0L..6L).map { io.github.dudu26.moneytick.core.fullDayPay(today.plusDays(it), c) }.firstOrNull { it > 0 } ?: 0.0
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge)
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
    )
}

@Composable
private fun BigAmountInput(value: String, symbol: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().softCard(RoundedCornerShape(28.dp)).padding(horizontal = 22.dp, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (symbol.isNotEmpty()) Text(symbol, style = NumberStyle.copy(fontSize = 28.sp), modifier = Modifier.padding(end = 6.dp))
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }.take(10)) },
            singleLine = true,
            textStyle = NumberStyle.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Start),
            cursorBrush = SolidColor(extraColors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(220.dp),
        )
    }
}

@Composable
private fun TimeTile(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.softCard().clickable(onClick = onClick).padding(18.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = NumberStyle.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
    }
}
