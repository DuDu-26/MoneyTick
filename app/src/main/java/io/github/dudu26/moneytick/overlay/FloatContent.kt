// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
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
import io.github.dudu26.moneytick.ui.paydayText
import io.github.dudu26.moneytick.ui.rememberPayday
import io.github.dudu26.moneytick.ui.statusText

/**
 * What the floating window draws. Also used as a live preview on the settings screen.
 * Deliberately has no animations: an overlay is on screen all day, so every frame costs battery.
 */
@Composable
fun FloatContent(settings: AppSettings, snapshot: SalarySnapshot, modifier: Modifier = Modifier) {
    val f = settings.floatConfig
    val fg = Color(f.textArgb.toInt())
    val scale = f.fontScale
    val payday = if (FloatItem.PAYDAY in f.items) rememberPayday(settings, snapshot) else null
    val transparent = f.transparentBackground
    // Without a card behind it the text sits straight on top of other apps, so give it a soft
    // halo in the opposite brightness to stay readable on any wallpaper.
    val shadow = if (transparent) {
        Shadow(
            color = (if (fg.luminance() > 0.5f) Color.Black else Color.White).copy(alpha = 0.9f),
            offset = Offset(0f, 0f), blurRadius = 10f,
        )
    } else {
        null
    }
    val shape = RoundedCornerShape(f.cornerDp.dp)
    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(f.opacity)
            .then(
                if (transparent) Modifier
                else Modifier.clip(shape).background(Color(f.backgroundArgb.toInt())).border(1.dp, fg.copy(alpha = 0.08f), shape),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (item in f.items) {
            when (item) {
                FloatItem.AMOUNT -> Text(
                    formatAmount(snapshot.earnedToday, settings.currencySymbol),
                    color = fg, fontSize = (22 * scale).sp, fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = "tnum", shadow = shadow), maxLines = 1, overflow = TextOverflow.Clip,
                )
                FloatItem.STATUS -> SmallText(statusText(snapshot), fg, scale, shadow)
                FloatItem.PROGRESS -> LinearProgressIndicator(
                    progress = { snapshot.progress.toFloat() },
                    color = fg, trackColor = fg.copy(alpha = if (transparent) 0.3f else 0.15f),
                    strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).height((4 * scale).dp),
                )
                FloatItem.REMAINING -> countdownLabel(snapshot)?.let { label ->
                    val value = when {
                        label == "今日完成" -> "${(snapshot.progress * 100).toInt()}%"
                        else -> formatDuration(snapshot.nextTransitionMs)
                    }
                    SmallText("$label $value", fg, scale, shadow)
                }
                FloatItem.SECOND_RATE -> SmallText(
                    "秒薪 " + formatAmount(snapshot.secondRate, settings.currencySymbol, 4), fg, scale, shadow,
                )
                FloatItem.PAYDAY -> payday?.let { SmallText(paydayText(it.daysUntil), fg, scale, shadow) }
                FloatItem.DAILY_TARGET -> SmallText(
                    "今日预计 " + formatAmount(snapshot.dailySalary, settings.currencySymbol), fg, scale, shadow,
                )
            }
        }
    }
}

@Composable
private fun SmallText(text: String, color: Color, scale: Float, shadow: Shadow?) =
    Text(
        text, color = color.copy(alpha = 0.85f), fontSize = (12 * scale).sp, maxLines = 1,
        overflow = TextOverflow.Ellipsis, style = TextStyle(shadow = shadow),
    )
