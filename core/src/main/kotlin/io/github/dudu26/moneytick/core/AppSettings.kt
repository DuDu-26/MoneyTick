// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only
//
// Additional terms: see /legal/ADDITIONAL_TERMS.md

package io.github.dudu26.moneytick.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Items the user can choose to show in the floating window, in display order. */
@Serializable
enum class FloatItem { AMOUNT, STATUS, PROGRESS, REMAINING, SECOND_RATE, DAILY_TARGET }

@Serializable
data class FloatConfig(
    /** Items shown, in order. AMOUNT alone matches PayDance's mini window. */
    val items: List<FloatItem> = listOf(FloatItem.AMOUNT),
    val widthDp: Int = 160,
    val heightDp: Int = 56,
    val fontScale: Float = 1f,
    /** 0.1–1.0, same range as PayDance's mini window opacity. */
    val opacity: Float = 0.9f,
    val backgroundArgb: Long = 0xFF1F2328,
    val textArgb: Long = 0xFFF59E0B,
    val cornerDp: Int = 16,
    val x: Int = 40,
    val y: Int = 200,
    /** Locked windows ignore drag and resize so they cannot be moved by accident. */
    val locked: Boolean = false,
) {
    fun normalized() = copy(
        items = items.distinct().ifEmpty { listOf(FloatItem.AMOUNT) },
        widthDp = widthDp.coerceIn(MIN_WIDTH_DP, MAX_WIDTH_DP),
        heightDp = heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP),
        fontScale = fontScale.coerceIn(0.5f, 3f),
        opacity = opacity.coerceIn(0.1f, 1f),
        cornerDp = cornerDp.coerceIn(0, 40),
    )

    companion object {
        const val MIN_WIDTH_DP = 72
        const val MAX_WIDTH_DP = 600
        const val MIN_HEIGHT_DP = 32
        const val MAX_HEIGHT_DP = 600
    }
}

@Serializable
enum class AmountAnimation { ROLLING, PLAIN }

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AppSettings(
    val schemaVersion: Int = SCHEMA_VERSION,
    val salary: SalaryConfig = SalaryConfig(),
    val currencySymbol: String = DEFAULT_CURRENCY_SYMBOL,
    val amountAnimation: AmountAnimation = AmountAnimation.ROLLING,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
    val floatConfig: FloatConfig = FloatConfig(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun encode(settings: AppSettings): String = json.encodeToString(serializer(), settings)

        /** Never throws: corrupt or missing data falls back to defaults. */
        fun decode(raw: String?): AppSettings {
            if (raw.isNullOrBlank()) return AppSettings()
            return runCatching { json.decodeFromString(serializer(), raw) }
                .getOrDefault(AppSettings())
                .let { it.copy(
                    currencySymbol = sanitizeCurrencySymbol(it.currencySymbol),
                    floatConfig = it.floatConfig.normalized(),
                    salary = it.salary.copy(workdays = it.salary.workdays.filter { d -> d in 0..6 }.distinct().sorted()),
                ) }
        }
    }
}
