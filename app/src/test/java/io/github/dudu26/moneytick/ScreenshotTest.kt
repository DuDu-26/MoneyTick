// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.FloatConfig
import io.github.dudu26.moneytick.core.FloatItem
import io.github.dudu26.moneytick.core.SalaryConfig
import io.github.dudu26.moneytick.overlay.FloatContent
import io.github.dudu26.moneytick.ui.DashboardScreen
import io.github.dudu26.moneytick.ui.FloatSettingsScreen
import io.github.dudu26.moneytick.ui.LocalNow
import io.github.dudu26.moneytick.ui.OnboardingScreen
import io.github.dudu26.moneytick.ui.SalarySettingsScreen
import io.github.dudu26.moneytick.ui.rememberSalarySnapshot
import io.github.dudu26.moneytick.ui.theme.MoneyTickTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDateTime

/** Renders the main screens at a fixed time into docs/screenshots for the README. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "zh-rCN-w400dp-h880dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    // Wednesday afternoon, payday on the 10th.
    private val now = LocalDateTime.of(2026, 11, 18, 15, 42, 10)
    private val settings = AppSettings(
        salary = SalaryConfig(
            monthlySalary = 18000.0, workDaysPerMonth = 21.75, startTime = "09:30", endTime = "18:30",
            enableLunchBreak = true, lunchStart = "12:00", lunchEnd = "13:30",
        ),
        paydayOfMonth = 10,
        onboardingDone = true,
        notificationEnabled = true,
        floatConfig = FloatConfig(
            items = listOf(FloatItem.AMOUNT, FloatItem.PROGRESS, FloatItem.REMAINING),
            widthDp = 190, heightDp = 84,
        ),
    )

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalNow provides { now }) {
                MoneyTickTheme(dark) { content() }
            }
        }
        repeat(30) { compose.mainClock.advanceTimeByFrame() }
        val dir = File(System.getProperty("user.dir"), "../docs/screenshots").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(dir, "$name.png").path)
    }

    @Test fun dashboard() = shot("dashboard") { DashboardScreen(settings, onNavigate = {}) }

    @Test fun dashboardDark() = shot("dashboard-dark", dark = true) { DashboardScreen(settings, onNavigate = {}) }

    @Test fun floatSettings() = shot("float-settings") { FloatSettingsScreen(settings, onChange = {}, onBack = {}) }

    @Test fun salarySettings() = shot("salary-settings") { SalarySettingsScreen(settings, onSave = {}, onBack = {}) }

    @Test fun onboarding() = shot("onboarding") { OnboardingScreen(settings, onFinish = {}) }

    @Test fun onboardingHours() = shot("onboarding-hours") { OnboardingScreen(settings, onFinish = {}, initialStep = 1) }

    /** The floating window on top of a mock chat app, to show what it looks like in use. */
    @Test fun floatingWindow() = shot("floating-window") {
        val snapshot = rememberSalarySnapshot(settings.salary)
        Box(Modifier.fillMaxSize().background(Color(0xFFEDEDED))) {
            Column(Modifier.fillMaxSize().padding(top = 60.dp, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(0.62f, 0.45f, 0.7f, 0.38f, 0.55f, 0.66f, 0.42f).forEachIndexed { i, w ->
                    val mine = i % 3 == 1
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) androidx.compose.ui.Alignment.CenterEnd else androidx.compose.ui.Alignment.CenterStart) {
                        Box(
                            Modifier.fillMaxWidth(w).height(44.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (mine) Color(0xFF95EC69) else Color.White),
                        )
                    }
                }
            }
            Box(Modifier.offset(x = 190.dp, y = 120.dp).size(settings.floatConfig.widthDp.dp, settings.floatConfig.heightDp.dp)) {
                FloatContent(settings, snapshot)
            }
        }
    }
}
