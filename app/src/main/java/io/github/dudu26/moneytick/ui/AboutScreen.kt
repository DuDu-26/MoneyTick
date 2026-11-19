// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dudu26.moneytick.ui.components.GroupCard
import io.github.dudu26.moneytick.ui.components.LargeTitlePage
import io.github.dudu26.moneytick.ui.components.RowDivider
import io.github.dudu26.moneytick.ui.components.SettingRow

@Composable
fun AboutScreen(onBack: () -> Unit) = LargeTitlePage("关于", onBack) {
    GroupCard {
        SettingRow("摸鱼币 MoneyTick", "作者：dudu（GitHub @DuDu-26）")
        RowDivider()
        SettingRow("源代码", "github.com/DuDu-26/MoneyTick")
        RowDivider()
        SettingRow("隐私", "配置只保存在本机，不联网、不需要账号、无遥测")
    }
    // Short notice required by AGPL-3.0 §5(d) and PayDance legal/ADDITIONAL_TERMS.md §1. Keep it.
    Text(
        "Based on PayDance. Copyright (C) 2026 Mr.Baoboer.\n" +
            "Licensed under the GNU Affero General Public License v3.0 only.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp),
    )
}
