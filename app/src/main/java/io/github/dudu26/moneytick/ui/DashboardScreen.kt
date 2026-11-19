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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.formatAmount
import io.github.dudu26.moneytick.overlay.FloatingWindowService

@Composable
fun DashboardScreen(settings: AppSettings, onNavigate: (Screen) -> Unit) {
    val context = LocalContext.current
    val snapshot = rememberSalarySnapshot(settings.salary)
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

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(statusText(snapshot), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("今日入账", style = MaterialTheme.typography.titleMedium)
            Text(
                formatAmount(snapshot.earnedToday, symbol),
                fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            )
            LinearProgressIndicator(progress = { snapshot.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow("今日进度", "${"%.1f".format(snapshot.progress * 100)}%")
                    StatRow("已工作", formatDuration(snapshot.elapsedWorkMs))
                    countdownLabel(snapshot)?.let { label ->
                        if (label != "今日完成") StatRow(label, formatDuration(snapshot.nextTransitionMs))
                    }
                    StatRow("今日预计", formatAmount(snapshot.dailySalary, symbol))
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("薪资说明", style = MaterialTheme.typography.titleSmall)
                    StatRow("时薪", formatAmount(snapshot.hourlyRate, symbol))
                    StatRow("分薪", formatAmount(snapshot.minuteRate, symbol, 3))
                    StatRow("秒薪", formatAmount(snapshot.secondRate, symbol, 4))
                }
            }
            Button(onClick = { toggleFloat() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (floatRunning) "关闭悬浮窗" else "开启悬浮窗")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onNavigate(Screen.SALARY) }, modifier = Modifier.weight(1f)) { Text("薪资设置") }
                OutlinedButton(onClick = { onNavigate(Screen.FLOAT) }, modifier = Modifier.weight(1f)) { Text("悬浮窗设置") }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { onNavigate(Screen.ABOUT) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("关于与许可")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace)
    }
}
