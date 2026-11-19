// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Emerald = Color(0xFF0FB57A)
val Gold = Color(0xFFF5A524)

@Immutable
data class ExtraColors(
    val heroBrush: Brush,
    val onHero: Color,
    val card: Color,
    val divider: Color,
    val gold: Color,
    val subtle: Color,
)

private val LightExtra = ExtraColors(
    heroBrush = Brush.linearGradient(listOf(Color(0xFF12C48B), Color(0xFF0EA5A0), Color(0xFF1C82D6))),
    onHero = Color.White,
    card = Color.White,
    divider = Color(0xFFEDEEF1),
    gold = Gold,
    subtle = Color(0xFFF1F3F6),
)

private val DarkExtra = ExtraColors(
    heroBrush = Brush.linearGradient(listOf(Color(0xFF0B8F64), Color(0xFF0A7A7E), Color(0xFF155E9C))),
    onHero = Color.White,
    card = Color(0xFF17191D),
    divider = Color(0xFF26292E),
    gold = Color(0xFFFFC14D),
    subtle = Color(0xFF202328),
)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

private val LightScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F7EA),
    onPrimaryContainer = Color(0xFF00513A),
    secondary = Color(0xFF1C82D6),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF111318),
    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFF1F3F6),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE5E7EB),
    error = Color(0xFFE5484D),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0B3D2E),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF60A5FA),
    background = Color(0xFF0B0C0F),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF0B0C0F),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF202328),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF2A2D33),
    error = Color(0xFFFF6369),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Tabular figures keep digits the same width so a ticking amount does not jitter. */
val NumberStyle = TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")

@Composable
fun MoneyTickTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

val extraColors: ExtraColors
    @Composable get() = LocalExtraColors.current
