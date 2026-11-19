// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.FloatConfig
import io.github.dudu26.moneytick.core.FloatItem
import io.github.dudu26.moneytick.overlay.FloatContent
import io.github.dudu26.moneytick.ui.components.GroupCard
import io.github.dudu26.moneytick.ui.components.LargeTitlePage
import io.github.dudu26.moneytick.ui.components.RowDivider
import io.github.dudu26.moneytick.ui.components.SectionHeader
import io.github.dudu26.moneytick.ui.components.SettingRow
import io.github.dudu26.moneytick.ui.theme.extraColors
import kotlin.math.roundToInt

private val itemLabels = mapOf(
    FloatItem.AMOUNT to "今日入账金额",
    FloatItem.STATUS to "工作状态",
    FloatItem.PROGRESS to "今日进度条",
    FloatItem.REMAINING to "倒计时（距离下班等）",
    FloatItem.SECOND_RATE to "秒薪",
    FloatItem.DAILY_TARGET to "今日预计",
)

private val bgPresets = listOf(0xFF1F2328, 0xFF000000, 0xFFFFFFFF, 0xFF0FB57A, 0xFF1C82D6, 0xFF7C3AED, 0xFFE11D48)
private val textPresets = listOf(0xFFF5A524, 0xFFFFFFFF, 0xFF111318, 0xFF34D399, 0xFF60A5FA, 0xFFF472B6, 0xFFFDE047)

@Composable
fun FloatSettingsScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onBack: () -> Unit) {
    val f = settings.floatConfig
    fun edit(transform: (FloatConfig) -> FloatConfig) =
        onChange { it.copy(floatConfig = transform(it.floatConfig).normalized()) }

    LargeTitlePage("自定义悬浮窗", onBack) {
        Text(
            "改动即时生效。悬浮窗上拖动可移动，拖右下角可缩放，双击回到 App。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Live preview on a mock wallpaper
        val snapshot = rememberSalarySnapshot(settings.salary, settings.refreshRate)
        Box(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(maxOf(f.heightDp + 48, 160).coerceAtMost(360).dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF8EC5FC), Color(0xFFE0C3FC), Color(0xFFFBC2EB)))),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(f.widthDp.dp, f.heightDp.dp)) { FloatContent(settings, snapshot) }
        }

        SectionHeader("显示内容")
        GroupCard {
            val ordered = f.items + FloatItem.entries.filter { it !in f.items }
            ordered.forEachIndexed { row, item ->
                if (row > 0) RowDivider()
                val index = f.items.indexOf(item)
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = index >= 0,
                        onCheckedChange = { on -> edit { c -> c.copy(items = if (on) c.items + item else c.items - item) } },
                    )
                    Text(
                        itemLabels.getValue(item),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (index >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (index >= 0) {
                        IconButton(enabled = index > 0, onClick = { edit { c -> c.copy(items = c.items.swap(index, index - 1)) } }) {
                            Icon(Icons.Rounded.KeyboardArrowUp, "上移")
                        }
                        IconButton(enabled = index < f.items.lastIndex, onClick = { edit { c -> c.copy(items = c.items.swap(index, index + 1)) } }) {
                            Icon(Icons.Rounded.KeyboardArrowDown, "下移")
                        }
                    }
                }
            }
        }

        SectionHeader("大小与外观")
        GroupCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                LabeledSlider("宽度", "${f.widthDp} dp", f.widthDp.toFloat(), FloatConfig.MIN_WIDTH_DP.toFloat()..FloatConfig.MAX_WIDTH_DP.toFloat()) { v -> edit { it.copy(widthDp = v.roundToInt()) } }
                LabeledSlider("高度", "${f.heightDp} dp", f.heightDp.toFloat(), FloatConfig.MIN_HEIGHT_DP.toFloat()..FloatConfig.MAX_HEIGHT_DP.toFloat()) { v -> edit { it.copy(heightDp = v.roundToInt()) } }
                LabeledSlider("字号", "${"%.1f".format(f.fontScale)}×", f.fontScale, 0.5f..3f) { v -> edit { it.copy(fontScale = v) } }
                LabeledSlider("不透明度", "${(f.opacity * 100).roundToInt()}%", f.opacity, 0.1f..1f) { v -> edit { it.copy(opacity = v) } }
                LabeledSlider("圆角", "${f.cornerDp} dp", f.cornerDp.toFloat(), 0f..40f) { v -> edit { it.copy(cornerDp = v.roundToInt()) } }
            }
        }

        SectionHeader("颜色")
        GroupCard {
            Column(Modifier.padding(16.dp)) {
                Text("背景", style = MaterialTheme.typography.bodyLarge)
                ColorRow(bgPresets, f.backgroundArgb) { c -> edit { it.copy(backgroundArgb = c) } }
                Text("文字", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                ColorRow(textPresets, f.textArgb) { c -> edit { it.copy(textArgb = c) } }
            }
        }

        SectionHeader("其他")
        GroupCard {
            SettingRow(
                "锁定位置和大小", "防止误触拖动", Icons.Rounded.Lock,
                trailing = { Switch(checked = f.locked, onCheckedChange = { on -> edit { it.copy(locked = on) } }) },
            )
        }
    }
}

private fun <T> List<T>.swap(i: Int, j: Int): List<T> = toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }

@Composable
private fun LabeledSlider(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = current, onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth().height(36.dp))
}

@Composable
private fun ColorRow(presets: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 10.dp)) {
        presets.forEach { argb ->
            val color = Color(argb.toInt())
            val isSelected = argb == selected
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, extraColors.divider, CircleShape)
                    .clickable { onPick(argb) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    val light = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f) > 0.6f
                    Icon(Icons.Rounded.Check, null, tint = if (light) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
