// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
    @Test fun roundTrip() {
        val s = AppSettings(currencySymbol = "$", floatConfig = FloatConfig(items = listOf(FloatItem.AMOUNT, FloatItem.PROGRESS), widthDp = 200))
        assertEquals(s, AppSettings.decode(AppSettings.encode(s)))
    }

    @Test fun corruptFallsBackToDefaults() {
        assertEquals(AppSettings(), AppSettings.decode("{not json"))
        assertEquals(AppSettings(), AppSettings.decode(null))
    }

    @Test fun floatConfigIsClamped() {
        val f = FloatConfig(widthDp = 1, heightDp = 9999, opacity = 0f, items = emptyList()).normalized()
        assertEquals(FloatConfig.MIN_WIDTH_DP, f.widthDp)
        assertEquals(FloatConfig.MAX_HEIGHT_DP, f.heightDp)
        assertEquals(0.1f, f.opacity)
        assertEquals(listOf(FloatItem.AMOUNT), f.items)
    }

    @Test fun currencySymbolIsSanitized() {
        assertEquals("HK$", sanitizeCurrencySymbol(" HK​$ "))
        assertEquals("ABCD", sanitizeCurrencySymbol("ABCDEF"))
        assertEquals("¥1,234.50", formatAmount(1234.5, "¥"))
    }
}
