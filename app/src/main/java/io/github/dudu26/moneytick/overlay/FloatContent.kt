// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.FloatItem
import io.github.dudu26.moneytick.core.SalarySnapshot
import io.github.dudu26.moneytick.core.formatAmount
import io.github.dudu26.moneytick.ui.countdownLabel
import io.github.dudu26.moneytick.ui.formatDuration
import io.github.dudu26.moneytick.ui.statusText

/** What the floating window draws. Also used as a live preview on the settings screen. */
@Composable
fun FloatContent(settings: AppSettings, snapshot: SalarySnapshot, modifier: Modifier = Modifier) {
    val f = settings.floatConfig
    val fg = Color(f.textArgb.toInt())
    val scale = f.fontScale
    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(f.opacity)
            .clip(RoundedCornerShape(f.cornerDp.dp))
            .background(Color(f.backgroundArgb.toInt()))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (item in f.items) {
            when (item) {
                FloatItem.AMOUNT -> Text(
                    formatAmount(snapshot.earnedToday, settings.currencySymbol),
                    color = fg, fontSize = (22 * scale).sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Clip,
                )
                FloatItem.STATUS -> SmallText(statusText(snapshot), fg, scale)
                FloatItem.PROGRESS -> LinearProgressIndicator(
                    progress = { snapshot.progress.toFloat() },
                    color = fg, trackColor = fg.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).height((4 * scale).dp),
                )
                FloatItem.REMAINING -> countdownLabel(snapshot)?.let { label ->
                    val value = when {
                        label == "今日完成" -> "${(snapshot.progress * 100).toInt()}%"
                        else -> formatDuration(snapshot.nextTransitionMs)
                    }
                    SmallText("$label $value", fg, scale)
                }
                FloatItem.SECOND_RATE -> SmallText(
                    "秒薪 " + formatAmount(snapshot.secondRate, settings.currencySymbol, 4), fg, scale,
                )
                FloatItem.DAILY_TARGET -> SmallText(
                    "今日预计 " + formatAmount(snapshot.dailySalary, settings.currencySymbol), fg, scale,
                )
            }
        }
    }
}

@Composable
private fun SmallText(text: String, color: Color, scale: Float) =
    Text(text, color = color.copy(alpha = 0.85f), fontSize = (12 * scale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
