// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import io.github.dudu26.moneytick.ui.theme.NumberStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.DoubleTapAction
import io.github.dudu26.moneytick.core.FloatConfig
import io.github.dudu26.moneytick.core.FloatItem
import io.github.dudu26.moneytick.overlay.FloatContent
import io.github.dudu26.moneytick.ui.components.GroupCard
import io.github.dudu26.moneytick.ui.components.LargeTitlePage
import io.github.dudu26.moneytick.ui.components.RowDivider
import io.github.dudu26.moneytick.ui.components.SectionHeader
import io.github.dudu26.moneytick.ui.components.SegmentedPicker
import io.github.dudu26.moneytick.ui.components.SettingRow
import io.github.dudu26.moneytick.ui.theme.extraColors
import io.github.dudu26.moneytick.ui.theme.softCard
import kotlin.math.roundToInt

private val itemLabels = linkedMapOf(
    FloatItem.AMOUNT to "今日入账",
    FloatItem.STATUS to "工作状态",
    FloatItem.PROGRESS to "进度条",
    FloatItem.REMAINING to "倒计时",
    FloatItem.SECOND_RATE to "秒薪",
    FloatItem.DAILY_TARGET to "今日预计",
    FloatItem.PAYDAY to "发薪倒计时",
)

private data class StylePreset(val name: String, val bg: Long, val fg: Long, val opacity: Float, val transparent: Boolean = false)

private val presets = listOf(
    StylePreset("清透白", 0xFFFFFFFF, 0xFF111215, 0.92f),
    StylePreset("曜石黑", 0xFF111215, 0xFFFFFFFF, 0.9f),
    StylePreset("暖橙", 0xFFFFF1E2, 0xFFE07A1F, 0.95f),
    StylePreset("极简", 0xFF000000, 0xFFFFFFFF, 0.35f),
    StylePreset("透明", 0xFFFFFFFF, 0xFF111215, 1f, transparent = true),
)

private val bgSwatches = listOf(0xFFFFFFFF, 0xFFF3F4F6, 0xFF111215, 0xFFFFF1E2, 0xFFE8F3FF, 0xFFEAF7EF)
private val fgSwatches = listOf(0xFF111215, 0xFFFFFFFF, 0xFFF59A3C, 0xFF2F80ED, 0xFF16A34A, 0xFFE5484D)

@Composable
fun FloatSettingsScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onBack: () -> Unit) {
    val f = settings.floatConfig
    fun edit(transform: (FloatConfig) -> FloatConfig) =
        onChange { it.copy(floatConfig = transform(it.floatConfig).normalized()) }
    val c = extraColors

    LargeTitlePage("自定义悬浮窗", onBack) {
        Text(
            "改动即时生效。在桌面上拖动悬浮窗可以挪位置，大小在这里调。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Preview stage
        val snapshot = rememberSalarySnapshot(settings.salary, settings.refreshRate)
        Box(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height((f.heightDp + 72).coerceIn(180, 380).dp)
                .softCard(RoundedCornerShape(28.dp))
                .padding(10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.subtle),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(f.widthDp.dp, f.heightDp.dp)) { FloatContent(settings, snapshot) }
            Text(
                "实时预览",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
        }

        SectionHeader("风格")
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            presets.forEach { p ->
                val selected = f.transparentBackground == p.transparent && f.textArgb == p.fg &&
                    (p.transparent || f.backgroundArgb == p.bg) && kotlin.math.abs(f.opacity - p.opacity) < 0.01f
                Column(
                    Modifier
                        .width(108.dp)
                        .softCard(RoundedCornerShape(20.dp), elevation = 10.dp)
                        .then(if (selected) Modifier.border(2.dp, c.ink, RoundedCornerShape(20.dp)) else Modifier)
                        .clickable {
                            edit {
                                it.copy(
                                    backgroundArgb = if (p.transparent) it.backgroundArgb else p.bg,
                                    textArgb = p.fg, opacity = p.opacity, transparentBackground = p.transparent,
                                )
                            }
                        }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(c.subtle),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .then(if (p.transparent) Modifier else Modifier.background(Color(p.bg.toInt()).copy(alpha = p.opacity)))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "¥128.50", color = Color(p.fg.toInt()), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                style = if (p.transparent) TextStyle(shadow = Shadow(Color.White, Offset(0f, 1f), 6f)) else TextStyle.Default,
                            )
                        }
                    }
                    Text(p.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        SectionHeader("显示内容")
        val entries = itemLabels.entries.toList()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.chunked(3).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { (item, label) ->
                        val on = item in f.items
                        Row(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .then(
                                    if (on) Modifier.clip(RoundedCornerShape(16.dp)).background(c.ink)
                                    else Modifier.softCard(RoundedCornerShape(16.dp), elevation = 6.dp),
                                )
                                .clickable { edit { cf -> cf.copy(items = if (on) cf.items - item else cf.items + item) } }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (on) Icon(Icons.Rounded.Check, null, tint = c.onInk, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                            Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) c.onInk else MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        if (f.items.size > 1) {
            SectionHeader("显示顺序")
            GroupCard {
                f.items.forEachIndexed { index, item ->
                    if (index > 0) RowDivider()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = c.accent, modifier = Modifier.width(24.dp))
                        Text(itemLabels.getValue(item), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        RoundIcon(Icons.Rounded.KeyboardArrowUp, "上移", enabled = index > 0) {
                            edit { cf -> cf.copy(items = cf.items.swap(index, index - 1)) }
                        }
                        Spacer(Modifier.width(8.dp))
                        RoundIcon(Icons.Rounded.KeyboardArrowDown, "下移", enabled = index < f.items.lastIndex) {
                            edit { cf -> cf.copy(items = cf.items.swap(index, index + 1)) }
                        }
                    }
                }
            }
        }

        SectionHeader("尺寸与外观")
        GroupCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PillSlider("宽度", "${f.widthDp} dp", f.widthDp.toFloat(), FloatConfig.MIN_WIDTH_DP.toFloat()..FloatConfig.MAX_WIDTH_DP.toFloat()) { v -> edit { it.copy(widthDp = v.roundToInt()) } }
                PillSlider("高度", "${f.heightDp} dp", f.heightDp.toFloat(), FloatConfig.MIN_HEIGHT_DP.toFloat()..FloatConfig.MAX_HEIGHT_DP.toFloat()) { v -> edit { it.copy(heightDp = v.roundToInt()) } }
                PillSlider("字号", "${"%.1f".format(f.fontScale)}×", f.fontScale, 0.5f..3f) { v -> edit { it.copy(fontScale = (v * 10).roundToInt() / 10f) } }
                PillSlider("不透明度", "${(f.opacity * 100).roundToInt()}%", f.opacity, 0.1f..1f) { v -> edit { it.copy(opacity = (v * 100).roundToInt() / 100f) } }
                if (!f.transparentBackground) {
                    PillSlider("圆角", "${f.cornerDp} dp", f.cornerDp.toFloat(), 0f..40f) { v -> edit { it.copy(cornerDp = v.roundToInt()) } }
                }
            }
        }

        SectionHeader("颜色")
        GroupCard {
            Column(Modifier.padding(16.dp)) {
                Text("背景", style = MaterialTheme.typography.bodyLarge)
                Swatches(
                    bgSwatches, if (f.transparentBackground) null else f.backgroundArgb,
                    transparentSelected = f.transparentBackground,
                    onTransparent = { edit { it.copy(transparentBackground = true) } },
                ) { col -> edit { it.copy(backgroundArgb = col, transparentBackground = false) } }
                Text("文字", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp))
                Swatches(fgSwatches, f.textArgb) { col -> edit { it.copy(textArgb = col) } }
                if (f.transparentBackground) {
                    Text(
                        "透明背景下文字会带一圈淡淡的描边，放在哪都看得清",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        SectionHeader("交互")
        GroupCard {
            Column(Modifier.padding(16.dp)) {
                Text("双击悬浮窗", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "隐藏后回到 App 首页，点黑色按钮即可重新显示",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )
                SegmentedPicker(
                    listOf(DoubleTapAction.HIDE to "隐藏悬浮窗", DoubleTapAction.OPEN_APP to "打开 App"),
                    f.doubleTapAction, { a -> edit { it.copy(doubleTapAction = a) } },
                )
            }
            RowDivider()
            SettingRow(
                "锁定位置", "防止误触拖动", Icons.Rounded.Lock,
                trailing = { Switch(checked = f.locked, onCheckedChange = { on -> edit { it.copy(locked = on) } }) },
            )
        }
    }
}

private fun <T> List<T>.swap(i: Int, j: Int): List<T> = toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val c = extraColors
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(c.subtle).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = if (enabled) c.ink else c.ink.copy(alpha = 0.25f), modifier = Modifier.size(20.dp))
    }
}

/**
 * A thick pill slider in the style of phone control centres: the label and value sit inside the
 * track, and the dark fill grows from the left. Text under the fill flips to the light colour so
 * it stays readable at any position. Drag anywhere to adjust relatively; tap to jump.
 */
@Composable
private fun PillSlider(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    val c = extraColors
    val height = 52.dp
    val shape = RoundedCornerShape(percent = 50)
    val span = range.endInclusive - range.start
    val fraction = ((current - range.start) / span).coerceIn(0f, 1f)
    val latestValue by rememberUpdatedState(current)
    val latestOnChange by rememberUpdatedState(onChange)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(c.subtle)
            .semantics { contentDescription = "$label $value" }
            .pointerInput(range) {
                detectTapGestures { o ->
                    latestOnChange(range.start + span * (o.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(range) {
                var dragged = 0f
                detectHorizontalDragGestures(onDragStart = { dragged = latestValue }) { change, dx ->
                    change.consume()
                    dragged = (dragged + dx / size.width * span).coerceIn(range.start, range.endInclusive)
                    latestOnChange(dragged)
                }
            },
    ) {
        val full = maxWidth
        val fillWidth = height + (full - height) * fraction
        PillLabels(label, value, c.ink, full)
        Box(Modifier.fillMaxHeight().width(fillWidth).clip(shape).background(c.ink)) {
            Box(Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) {
                PillLabels(label, value, c.onInk, full)
            }
        }
    }
}

@Composable
private fun PillLabels(label: String, value: String, color: Color, width: Dp) {
    Row(
        Modifier.width(width).fillMaxHeight().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = color, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, color = color.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium.merge(NumberStyle))
    }
}

@Composable
private fun Swatches(
    colors: List<Long>,
    selected: Long?,
    transparentSelected: Boolean = false,
    onTransparent: (() -> Unit)? = null,
    onPick: (Long) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
    ) {
        if (onTransparent != null) {
            SwatchDot(transparentSelected, onTransparent) {
                // Checkerboard: the usual "nothing here" pattern.
                Canvas(Modifier.fillMaxSize()) {
                    val cell = size.width / 4
                    drawRect(Color.White)
                    for (i in 0 until 4) for (j in 0 until 4) {
                        if ((i + j) % 2 == 0) drawRect(Color(0xFFD9DCE1), Offset(i * cell, j * cell), Size(cell, cell))
                    }
                }
            }
        }
        colors.forEach { argb ->
            SwatchDot(argb == selected, { onPick(argb) }) {
                Box(Modifier.fillMaxSize().background(Color(argb.toInt())))
            }
        }
    }
}

@Composable
private fun SwatchDot(selected: Boolean, onClick: () -> Unit, fill: @Composable () -> Unit) {
    val c = extraColors
    Box(
        Modifier
            .size(36.dp)
            .then(if (selected) Modifier.border(2.dp, c.ink, CircleShape).padding(4.dp) else Modifier)
            .clip(CircleShape)
            .border(1.dp, c.cardBorder, CircleShape)
            .clickable(onClick = onClick),
    ) {
        fill()
    }
}
