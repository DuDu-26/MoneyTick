// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.FloatConfig
import io.github.dudu26.moneytick.core.FloatItem
import io.github.dudu26.moneytick.overlay.FloatContent
import kotlin.math.roundToInt

private val itemLabels = mapOf(
    FloatItem.AMOUNT to "今日入账金额",
    FloatItem.STATUS to "工作状态",
    FloatItem.PROGRESS to "今日进度条",
    FloatItem.REMAINING to "倒计时（距离下班等）",
    FloatItem.SECOND_RATE to "秒薪",
    FloatItem.DAILY_TARGET to "今日预计",
)

private val bgPresets = listOf(0xFF1F2328, 0xFF000000, 0xFFFFFFFF, 0xFF0F766E, 0xFF1E3A8A, 0xFF7C2D12)
private val textPresets = listOf(0xFFF59E0B, 0xFFFFFFFF, 0xFF000000, 0xFF34D399, 0xFF60A5FA, 0xFFF472B6)

@Composable
fun FloatSettingsScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onBack: () -> Unit) {
    val f = settings.floatConfig
    fun edit(transform: (FloatConfig) -> FloatConfig) =
        onChange { it.copy(floatConfig = transform(it.floatConfig).normalized()) }

    SubScreen("悬浮窗设置", onBack) {
        Text("修改即时生效。悬浮窗上：拖动移动位置，拖右下角调整大小，双击打开 App。",
            style = MaterialTheme.typography.bodySmall)

        SectionTitle("预览")
        val snapshot = rememberSalarySnapshot(settings.salary)
        Box(Modifier.size(f.widthDp.dp, f.heightDp.dp)) { FloatContent(settings, snapshot) }

        SectionTitle("显示内容（勾选并排序）")
        val ordered = f.items + FloatItem.entries.filter { it !in f.items }
        ordered.forEach { item ->
            val index = f.items.indexOf(item)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = index >= 0,
                    onCheckedChange = { on -> edit { c -> c.copy(items = if (on) c.items + item else c.items - item) } },
                )
                Text(itemLabels.getValue(item), Modifier.weight(1f))
                if (index >= 0) {
                    TextButton(enabled = index > 0, onClick = { edit { c -> c.copy(items = c.items.swap(index, index - 1)) } }) { Text("上移") }
                    TextButton(enabled = index < f.items.lastIndex, onClick = { edit { c -> c.copy(items = c.items.swap(index, index + 1)) } }) { Text("下移") }
                }
            }
        }

        SectionTitle("大小与外观")
        LabeledSlider("宽度 ${f.widthDp}dp", f.widthDp.toFloat(), FloatConfig.MIN_WIDTH_DP.toFloat()..FloatConfig.MAX_WIDTH_DP.toFloat()) { v -> edit { it.copy(widthDp = v.roundToInt()) } }
        LabeledSlider("高度 ${f.heightDp}dp", f.heightDp.toFloat(), FloatConfig.MIN_HEIGHT_DP.toFloat()..FloatConfig.MAX_HEIGHT_DP.toFloat()) { v -> edit { it.copy(heightDp = v.roundToInt()) } }
        LabeledSlider("字号 ${"%.1f".format(f.fontScale)}x", f.fontScale, 0.5f..3f) { v -> edit { it.copy(fontScale = v) } }
        LabeledSlider("不透明度 ${(f.opacity * 100).roundToInt()}%", f.opacity, 0.1f..1f) { v -> edit { it.copy(opacity = v) } }
        LabeledSlider("圆角 ${f.cornerDp}dp", f.cornerDp.toFloat(), 0f..40f) { v -> edit { it.copy(cornerDp = v.roundToInt()) } }

        Text("背景颜色", Modifier.padding(top = 8.dp))
        ColorRow(bgPresets, f.backgroundArgb) { c -> edit { it.copy(backgroundArgb = c) } }
        Text("文字颜色", Modifier.padding(top = 8.dp))
        ColorRow(textPresets, f.textArgb) { c -> edit { it.copy(textArgb = c) } }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            Text("锁定位置和大小（防误触）", Modifier.weight(1f))
            Switch(checked = f.locked, onCheckedChange = { on -> edit { it.copy(locked = on) } })
        }
    }
}

private fun <T> List<T>.swap(i: Int, j: Int): List<T> = toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Text(label)
    Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth().height(40.dp))
}

@Composable
private fun ColorRow(presets: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        presets.forEach { argb ->
            Box(
                Modifier.size(32.dp).background(Color(argb.toInt()), CircleShape)
                    .border(if (argb == selected) 3.dp else 1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onPick(argb) },
            )
        }
    }
}
