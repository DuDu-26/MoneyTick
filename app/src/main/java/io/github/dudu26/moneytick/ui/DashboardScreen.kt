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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dudu26.moneytick.core.AmountAnimation
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.SalarySnapshot
import io.github.dudu26.moneytick.core.SalaryStatus
import io.github.dudu26.moneytick.core.formatAmount
import io.github.dudu26.moneytick.overlay.FloatingWindowService
import io.github.dudu26.moneytick.ui.components.GroupCard
import io.github.dudu26.moneytick.ui.components.RollingText
import io.github.dudu26.moneytick.ui.components.RowDivider
import io.github.dudu26.moneytick.ui.components.SectionHeader
import io.github.dudu26.moneytick.ui.components.SettingRow
import io.github.dudu26.moneytick.ui.components.StatusPill
import io.github.dudu26.moneytick.ui.theme.NumberStyle
import io.github.dudu26.moneytick.ui.theme.extraColors

@Composable
fun DashboardScreen(settings: AppSettings, onNavigate: (Screen) -> Unit) {
    val context = LocalContext.current
    val snapshot = rememberSalarySnapshot(settings.salary, settings.refreshRate)
    val floatRunning by FloatingWindowService.running.collectAsStateWithLifecycle()
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
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            // Header
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("摸鱼币", style = MaterialTheme.typography.headlineSmall)
                    Text(greeting(snapshot), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onNavigate(Screen.SALARY) }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "设置")
                }
            }
            Spacer(Modifier.height(16.dp))

            HeroCard(snapshot, symbol, settings.amountAnimation == AmountAnimation.ROLLING)

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("已工作", formatDuration(snapshot.elapsedWorkMs), Modifier.weight(1f))
                StatTile("今日预计", formatAmount(snapshot.dailySalary, symbol), Modifier.weight(1f), extraColors.gold)
            }

            SectionHeader("薪资换算")
            GroupCard {
                Row(Modifier.padding(vertical = 16.dp)) {
                    RateCell("时薪", formatAmount(snapshot.hourlyRate, symbol), Modifier.weight(1f))
                    RateCell("分薪", formatAmount(snapshot.minuteRate, symbol, 3), Modifier.weight(1f))
                    RateCell("秒薪", formatAmount(snapshot.secondRate, symbol, 4), Modifier.weight(1f))
                }
            }

            SectionHeader("悬浮窗")
            GroupCard {
                SettingRow(
                    title = "桌面悬浮窗",
                    subtitle = if (floatRunning) "正在显示，可拖动、拖角缩放、双击返回" else "在任何 App 上实时看到今日入账",
                    icon = Icons.Rounded.Star,
                    iconTint = extraColors.gold,
                    trailing = { Switch(checked = floatRunning, onCheckedChange = { toggleFloat() }) },
                )
                RowDivider()
                SettingRow("自定义悬浮窗", "显示内容、大小、颜色、透明度", Icons.Rounded.Edit, onClick = { onNavigate(Screen.FLOAT) })
            }

            SectionHeader("更多")
            GroupCard {
                SettingRow("薪资与作息", workSummary(settings), Icons.Rounded.DateRange, onClick = { onNavigate(Screen.SALARY) })
                RowDivider()
                SettingRow("关于与许可", null, Icons.Rounded.Info, MaterialTheme.colorScheme.onSurfaceVariant, onClick = { onNavigate(Screen.ABOUT) })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroCard(snapshot: SalarySnapshot, symbol: String, rolling: Boolean) {
    val onHero = extraColors.onHero
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(extraColors.heroBrush)
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("今日入账", style = MaterialTheme.typography.titleSmall, color = onHero.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                StatusPill(
                    statusText(snapshot),
                    dotColor = if (snapshot.isWorking) Color(0xFFB9FFD9) else onHero.copy(alpha = 0.6f),
                    background = Color.White.copy(alpha = 0.18f),
                    contentColor = onHero,
                )
            }
            Spacer(Modifier.height(10.dp))
            RollingText(
                formatAmount(snapshot.earnedToday, symbol),
                style = NumberStyle.copy(fontSize = 44.sp, letterSpacing = (-0.5).sp),
                color = onHero,
                animate = rolling,
            )
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { snapshot.progress.toFloat() },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Text(
                    "今日进度 ${"%.1f".format(snapshot.progress * 100)}%",
                    style = MaterialTheme.typography.bodySmall, color = onHero.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                countdownLabel(snapshot)?.takeIf { it != "今日完成" }?.let { label ->
                    Text(
                        "$label ${formatDuration(snapshot.nextTransitionMs)}",
                        style = MaterialTheme.typography.bodySmall.merge(NumberStyle).copy(fontWeight = FontWeight.Medium),
                        color = onHero,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier, accent: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(extraColors.card).padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(value, style = NumberStyle.copy(fontSize = 20.sp), color = accent, maxLines = 1)
    }
}

@Composable
private fun RateCell(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NumberStyle.copy(fontSize = 16.sp), maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun greeting(s: SalarySnapshot): String = when (s.status) {
    SalaryStatus.WORKING -> if (s.isNightWork) "夜班辛苦了，每一秒都算数" else "认真摸鱼，钱也在涨"
    SalaryStatus.LUNCH_BREAK -> "午休中，好好吃饭"
    SalaryStatus.BEFORE_WORK -> "还没上班，再睡会儿"
    SalaryStatus.AFTER_WORK -> "下班啦，今天的钱到手了"
    SalaryStatus.REST_DAY -> "今天休息，放心玩"
    SalaryStatus.INVALID_CONFIG -> "先去设置一下薪资吧"
}

private fun workSummary(s: AppSettings): String {
    val c = s.salary
    val mode = when (c.salaryType) {
        io.github.dudu26.moneytick.core.SalaryType.MONTHLY -> "月薪"
        io.github.dudu26.moneytick.core.SalaryType.DAILY -> "日薪"
        io.github.dudu26.moneytick.core.SalaryType.HOURLY -> "时薪"
    }
    return "$mode · ${c.startTime}–${c.endTime}" + if (c.enableLunchBreak) " · 剔除午休" else ""
}
