// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun AboutScreen(onBack: () -> Unit) = SubScreen("关于与许可", onBack) {
    Text("摸鱼币 MoneyTick", style = MaterialTheme.typography.headlineSmall)
    Text("作者：dudu（GitHub @DuDu-26）", modifier = Modifier.padding(top = 4.dp))
    Text("本应用是 PayDance 的修改版安卓移植，并非 PayDance 官方版本，也未获得原作者背书。",
        modifier = Modifier.padding(vertical = 8.dp))
    // Notice required by PayDance legal/ADDITIONAL_TERMS.md §1. Keep it.
    Text(
        "Based on PayDance. Copyright (C) 2026 Mr.Baoboer.\n" +
            "Licensed under the GNU Affero General Public License v3.0 only.\n\n" +
            "基于 PayDance。版权所有 (C) 2026 Mr.Baoboer。\n" +
            "采用 GNU Affero General Public License v3.0 only 许可。",
        style = MaterialTheme.typography.bodySmall,
    )
    Text("源代码：https://github.com/DuDu-26/-MoneyTick\n原项目：https://github.com/MrBaoboer/PayDance",
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    Text("所有配置只保存在本机，不联网、不需要账号、无遥测。",
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
}
