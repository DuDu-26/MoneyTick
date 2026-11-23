// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "清透" style: flat light background, white cards with a soft wide shadow and a hairline
 * highlight border, big black numbers, one accent colour (orange). No gradients.
 */
@Immutable
data class ExtraColors(
    val card: Color,
    val cardBorder: Color,
    val shadow: Color,
    val divider: Color,
    val accent: Color,
    val accentSoft: Color,
    val subtle: Color,
    val ink: Color,
    val onInk: Color,
    /** Frosted-glass dock: translucent tint over the blurred page, a bright rim, and the selected "lens". */
    val glassTint: Color,
    val glassRim: Color,
    val glassLens: Color,
)

private val LightExtra = ExtraColors(
    card = Color.White,
    cardBorder = Color(0xFFF0F1F3),
    shadow = Color(0xFF8A94A6),
    divider = Color(0xFFF0F1F3),
    accent = Color(0xFFF59A3C),
    accentSoft = Color(0xFFFFF1E2),
    subtle = Color(0xFFF3F4F6),
    ink = Color(0xFF111215),
    onInk = Color.White,
    glassTint = Color.White.copy(alpha = 0.58f),
    glassRim = Color.White.copy(alpha = 0.9f),
    glassLens = Color.White.copy(alpha = 0.85f),
)

private val DarkExtra = ExtraColors(
    card = Color(0xFF1A1B1F),
    cardBorder = Color(0xFF26282D),
    shadow = Color.Black,
    divider = Color(0xFF26282D),
    accent = Color(0xFFFFAD5C),
    accentSoft = Color(0xFF3A2A1A),
    subtle = Color(0xFF24262B),
    ink = Color(0xFFF5F5F7),
    onInk = Color(0xFF111215),
    glassTint = Color(0xFF1A1B1F).copy(alpha = 0.6f),
    glassRim = Color.White.copy(alpha = 0.12f),
    glassLens = Color.White.copy(alpha = 0.10f),
)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF111215),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3F4F6),
    onPrimaryContainer = Color(0xFF111215),
    secondary = Color(0xFFF59A3C),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF111215),
    surface = Color(0xFFF5F6F8),
    onSurface = Color(0xFF111215),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF8A8F98),
    outline = Color(0xFFE6E8EB),
    error = Color(0xFFE5484D),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF5F5F7),
    onPrimary = Color(0xFF111215),
    primaryContainer = Color(0xFF24262B),
    onPrimaryContainer = Color(0xFFF5F5F7),
    secondary = Color(0xFFFFAD5C),
    background = Color(0xFF0E0F11),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF0E0F11),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF24262B),
    onSurfaceVariant = Color(0xFF8E939C),
    outline = Color(0xFF2A2C31),
    error = Color(0xFFFF6369),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.3).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Tabular figures keep digits the same width so a ticking amount does not jitter. */
val NumberStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", letterSpacing = (-0.5).sp)

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

/** White card with a soft, wide, low-contrast shadow and a hairline border. */
@Composable
fun Modifier.softCard(shape: Shape = RoundedCornerShape(24.dp), elevation: Dp = 18.dp): Modifier {
    val c = extraColors
    return this
        .shadow(elevation, shape, clip = false, ambientColor = c.shadow.copy(alpha = 0.10f), spotColor = c.shadow.copy(alpha = 0.16f))
        .clip(shape)
        .background(c.card)
        .border(1.dp, c.cardBorder, shape)
}
