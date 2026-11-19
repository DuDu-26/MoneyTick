// SPDX-FileCopyrightText: 2026 Mr.Baoboer (PayDance), 2026 dudu (DuDu-26) (Kotlin port and modifications)
// SPDX-License-Identifier: AGPL-3.0-only
//
// Additional terms: see /legal/ADDITIONAL_TERMS.md
// Ported from PayDance src/lib/currency.ts (commit e922603). Modified for Android.

package io.github.dudu26.moneytick.core

import java.util.Locale

const val DEFAULT_CURRENCY_SYMBOL = "¥"
const val MAX_CURRENCY_SYMBOL_LENGTH = 4

private val forbidden = Regex("[\\u0000-\\u001F\\u007F-\\u009F\\u200B-\\u200F\\u2028\\u2029\\uFEFF]")

/** Strips control/zero-width characters and limits length by code point. Empty = no symbol. */
fun sanitizeCurrencySymbol(value: String): String {
    val cleaned = value.replace(forbidden, "").trim()
    val cps = cleaned.codePoints().limit(MAX_CURRENCY_SYMBOL_LENGTH.toLong()).toArray()
    return String(cps, 0, cps.size)
}

fun formatAmount(amount: Double, symbol: String, decimals: Int = 2): String {
    val number = String.format(Locale.US, "%,.${decimals}f", amount)
    return if (symbol.isEmpty()) number else symbol + number
}
