// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dudu26.moneytick.core.AmountAnimation
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.PaydayInfo
import io.github.dudu26.moneytick.core.SalarySnapshot
import io.github.dudu26.moneytick.core.SalaryStatus
import io.github.dudu26.moneytick.core.SalaryType
import io.github.dudu26.moneytick.core.formatAmount
import io.github.dudu26.moneytick.overlay.FloatingWindowService
import io.github.dudu26.moneytick.ui.components.InkCircleButton
import io.github.dudu26.moneytick.ui.components.RollingText
import io.github.dudu26.moneytick.ui.theme.NumberStyle
import io.github.dudu26.moneytick.ui.theme.extraColors
import io.github.dudu26.moneytick.ui.theme.softCard
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    settings: AppSettings,
    onNavigate: (Screen) -> Unit,
    onToggleNotification: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val snapshot = rememberSalarySnapshot(settings.salary, settings.refreshRate)
    val floatRunning by FloatingWindowService.running.collectAsStateWithLifecycle()
    val payday = rememberPayday(settings, snapshot)
    val today = LocalNow.current().toLocalDate()
    val symbol = settings.currencySymbol
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        FloatingWindowService.start(context)
    }

    fun toggleFloat() {
        when {
            floatRunning -> FloatingWindowService.stop(context)
            !Settings.canDrawOverlays(context) -> context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> FloatingWindowService.start(context)
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(20.dp))
                Text(
                    today.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(greeting(snapshot), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(20.dp))

                EarningsCard(snapshot, symbol, settings.amountAnimation == AmountAnimation.ROLLING) {
                    onNavigate(Screen.SALARY)
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().softCard().padding(vertical = 18.dp)) {
                    MiniStat("时薪", formatAmount(snapshot.hourlyRate, symbol), Modifier.weight(1f))
                    MiniStat("分薪", formatAmount(snapshot.minuteRate, symbol, 3), Modifier.weight(1f))
                    MiniStat("秒薪", formatAmount(snapshot.secondRate, symbol, 4), Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))
                PaydayCard(payday, symbol, settings.paydayOfMonth) { onNavigate(Screen.SALARY) }

                Spacer(Modifier.height(16.dp))
                FloatCard(floatRunning, workSummary(settings), settings.notificationEnabled, onToggleNotification)
                Spacer(Modifier.height(140.dp)) // room for the dock
            }

            Dock(
                floatRunning = floatRunning,
                onToggleFloat = { toggleFloat() },
                onNavigate = onNavigate,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            )
        }
    }
}

@Composable
private fun EarningsCard(snapshot: SalarySnapshot, symbol: String, rolling: Boolean, onOpen: () -> Unit) {
    val c = extraColors
    Column(Modifier.fillMaxWidth().softCard(RoundedCornerShape(28.dp)).padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日入账", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (snapshot.isWorking) c.accent else MaterialTheme.colorScheme.outline))
                    Text(
                        statusText(snapshot),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (snapshot.isWorking) c.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Box(
                Modifier.size(40.dp).softCard(CircleShape, elevation = 8.dp).clickable(onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "薪资设置") }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            if (symbol.isNotEmpty()) {
                Text(symbol, style = NumberStyle.copy(fontSize = 24.sp), modifier = Modifier.padding(bottom = 8.dp, end = 4.dp))
            }
            RollingText(
                formatAmount(snapshot.earnedToday, ""),
                style = NumberStyle.copy(fontSize = 52.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                animate = rolling,
            )
        }
        Spacer(Modifier.height(22.dp))
        ProgressLine(snapshot.progress.toFloat())
        Spacer(Modifier.height(12.dp))
        Row {
            Text(
                "今日进度 ${"%.1f".format(snapshot.progress * 100)}%",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            countdownLabel(snapshot)?.takeIf { it != "今日完成" }?.let { label ->
                Text(label + " ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(snapshot.nextTransitionMs), style = MaterialTheme.typography.bodySmall.merge(NumberStyle), color = c.accent)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.subtle).padding(vertical = 14.dp)) {
            MiniStat("已工作", formatDuration(snapshot.elapsedWorkMs), Modifier.weight(1f))
            MiniStat("今日预计", formatAmount(snapshot.dailySalary, symbol), Modifier.weight(1f))
        }
    }
}

/** Thin accent progress line with an end dot, like the reference charts. */
@Composable
private fun ProgressLine(progress: Float) {
    val c = extraColors
    BoxWithConstraints(Modifier.fillMaxWidth().height(12.dp), contentAlignment = Alignment.CenterStart) {
        val p = progress.coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.subtle))
        Box(Modifier.width(maxWidth * p).height(4.dp).clip(CircleShape).background(c.accent))
        Box(
            Modifier
                .offset(x = (maxWidth - 12.dp) * p)
                .size(12.dp)
                .clip(CircleShape)
                .background(c.card)
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(c.accent),
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NumberStyle.copy(fontSize = 17.sp), maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun PaydayCard(payday: PaydayInfo?, symbol: String, dayOfMonth: Int, onSetup: () -> Unit) {
    val c = extraColors
    if (payday == null) {
        Row(
            Modifier.fillMaxWidth().softCard().clickable(onClick = onSetup).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("发薪倒计时", style = MaterialTheme.typography.titleMedium)
                Text("设置发薪日，看看还要熬几天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Row(Modifier.fillMaxWidth().softCard().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("距离发薪", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (payday.isToday) {
                Text("就是今天！", style = NumberStyle.copy(fontSize = 30.sp), color = c.accent, modifier = Modifier.padding(top = 2.dp))
            } else {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        "${payday.daysUntil}",
                        style = NumberStyle.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
                        color = if (payday.daysUntil <= 3) c.accent else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(" 天", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            Text(
                "每月 $dayOfMonth 日 · 下次 ${payday.nextPayday.monthValue}月${payday.nextPayday.dayOfMonth}日",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).background(c.subtle).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text("本期已挣", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatAmount(payday.periodEarned, symbol), style = NumberStyle.copy(fontSize = 18.sp), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun FloatCard(running: Boolean, schedule: String, notificationOn: Boolean, onToggleNotification: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().softCard()) {
        FloatStatusRow(running, schedule)
        HorizontalDivider(color = extraColors.divider, modifier = Modifier.padding(horizontal = 18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("通知栏显示金额", style = MaterialTheme.typography.bodyLarge)
                Text("下拉通知就能看，息屏时不刷新", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = notificationOn, onCheckedChange = onToggleNotification)
        }
    }
}

@Composable
private fun FloatStatusRow(running: Boolean, schedule: String) {
    val c = extraColors
    Row(
        Modifier.fillMaxWidth().padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(if (running) "悬浮窗已开启" else "悬浮窗已隐藏", style = MaterialTheme.typography.titleMedium)
            Text(
                if (running) "双击悬浮窗可隐藏，点下方黑色按钮也能关" else "点下方黑色按钮显示悬浮窗",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(schedule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
        Box(
            Modifier.clip(CircleShape).background(if (running) c.accentSoft else c.subtle).padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(if (running) "显示中" else "已隐藏", style = MaterialTheme.typography.labelMedium, color = if (running) c.accent else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Dock(floatRunning: Boolean, onToggleFloat: () -> Unit, onNavigate: (Screen) -> Unit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().height(76.dp).softCard(RoundedCornerShape(32.dp), elevation = 24.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockItem(Icons.Rounded.Home, "首页", selected = true, Modifier.weight(1f)) {}
        DockItem(Icons.Rounded.Edit, "悬浮窗", selected = false, Modifier.weight(1f)) { onNavigate(Screen.FLOAT) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            InkCircleButton(
                if (floatRunning) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
                if (floatRunning) "隐藏悬浮窗" else "显示悬浮窗",
                onToggleFloat,
                size = 58.dp,
            )
        }
        DockItem(Icons.Rounded.DateRange, "薪资", selected = false, Modifier.weight(1f)) { onNavigate(Screen.SALARY) }
        DockItem(Icons.Rounded.Info, "关于", selected = false, Modifier.weight(1f)) { onNavigate(Screen.ABOUT) }
    }
}

@Composable
private fun DockItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (selected) extraColors.accent else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, modifier = Modifier.padding(top = 3.dp))
    }
}

private fun greeting(s: SalarySnapshot): String = when (s.status) {
    SalaryStatus.WORKING -> if (s.isNightWork) "夜班辛苦，每秒都算数" else "认真摸鱼，钱也在涨"
    SalaryStatus.LUNCH_BREAK -> "午休中，好好吃饭"
    SalaryStatus.BEFORE_WORK -> "还没上班，再睡会儿"
    SalaryStatus.AFTER_WORK -> "下班啦，今天的钱到手了"
    SalaryStatus.REST_DAY -> "今天休息，放心玩"
    SalaryStatus.INVALID_CONFIG -> "先去设置一下薪资吧"
}

private fun workSummary(s: AppSettings): String {
    val c = s.salary
    val mode = when (c.salaryType) {
        SalaryType.MONTHLY -> "月薪"
        SalaryType.DAILY -> "日薪"
        SalaryType.HOURLY -> "时薪"
    }
    return "$mode · ${c.startTime}–${c.endTime}" + if (c.enableLunchBreak) " · 剔除午休" else ""
}
