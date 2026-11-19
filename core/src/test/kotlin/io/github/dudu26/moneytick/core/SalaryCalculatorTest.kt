// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SalaryCalculatorTest {
    // 2026-11-18 is a Wednesday, 2026-11-21 a Saturday.
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, day, h, m)
    private val base = SalaryConfig(monthlySalary = 22000.0, workDaysPerMonth = 22.0, startTime = "09:00", endTime = "18:00")

    @Test fun halfDayEarnsHalf() {
        val s = calculateSalarySnapshot(at(18, 13, 30), base)
        assertEquals(SalaryStatus.WORKING, s.status)
        assertEquals(1000.0, s.dailySalary, 1e-9)
        assertEquals(500.0, s.earnedToday, 1e-9)
    }

    @Test fun beforeAndAfterWork() {
        assertEquals(SalaryStatus.BEFORE_WORK, calculateSalarySnapshot(at(18, 8), base).status)
        val after = calculateSalarySnapshot(at(18, 19), base)
        assertEquals(SalaryStatus.AFTER_WORK, after.status)
        assertEquals(1000.0, after.earnedToday, 1e-9)
    }

    @Test fun weekendIsRestDay() {
        assertEquals(SalaryStatus.REST_DAY, calculateSalarySnapshot(at(21, 12), base).status)
    }

    @Test fun lunchBreakIsExcluded() {
        val c = base.copy(enableLunchBreak = true, lunchStart = "12:00", lunchEnd = "13:00")
        val s = calculateSalarySnapshot(at(18, 12, 30), c)
        assertEquals(SalaryStatus.LUNCH_BREAK, s.status)
        assertEquals(8 * 3_600_000L, s.workMsToday)
        assertEquals(375.0, s.earnedToday, 1e-9) // 3h of 8h
        assertEquals(30 * 60_000L, s.nextTransitionMs)
    }

    @Test fun hourlyUsesWorkedHours() {
        val c = base.copy(salaryType = SalaryType.HOURLY, hourlyRate = 50.0)
        val s = calculateSalarySnapshot(at(18, 10), c)
        assertEquals(450.0, s.dailySalary, 1e-9)
        assertEquals(50.0, s.earnedToday, 1e-9)
    }

    @Test fun overnightShiftContinuesIntoNextDay() {
        val c = base.copy(startTime = "22:00", endTime = "06:00", workdays = listOf(3)) // Wednesday night
        val s = calculateSalarySnapshot(at(19, 2), c) // Thursday 02:00
        assertEquals(SalaryStatus.WORKING, s.status)
        assertTrue(s.isNightWork)
        assertEquals(0.5, s.progress, 1e-9)
        val done = calculateSalarySnapshot(at(19, 7), c)
        assertEquals(SalaryStatus.AFTER_WORK, done.status)
        assertTrue(done.isNightWork)
    }

    @Test fun invalidConfigIsReported() {
        val c = base.copy(startTime = "09:00", endTime = "09:00")
        assertEquals(SalaryStatus.INVALID_CONFIG, calculateSalarySnapshot(at(18, 10), c).status)
        assertEquals(listOf(IssueCode.TIME_SAME), validateSalaryConfig(c).map { it.code })
    }

    @Test fun lunchOutsideWorkWindowIsInvalid() {
        val c = base.copy(enableLunchBreak = true, lunchStart = "19:00", lunchEnd = "20:00")
        assertEquals(listOf(IssueCode.LUNCH_OUTSIDE), validateSalaryConfig(c).map { it.code })
    }
}

class TickDelayTest {
    private val working = SalarySnapshot(status = SalaryStatus.WORKING, secondRate = 1000.0 / 32400)

    @Test fun realtimeFollowsCents() {
        // 1000/day over 9h ≈ 0.0309/s → one cent every ~324 ms
        assertEquals(324L, nextTickDelayMs(working))
    }

    @Test fun slowerRatesUseFixedInterval() {
        assertEquals(5_000L, nextTickDelayMs(working, RefreshRate.FIVE_SECONDS))
        assertEquals(60_000L, nextTickDelayMs(working, RefreshRate.MINUTE))
    }

    @Test fun idleWhenNothingChanges() {
        assertEquals(IDLE_TICK_MS, nextTickDelayMs(SalarySnapshot(status = SalaryStatus.AFTER_WORK)))
        assertEquals(IDLE_TICK_MS, nextTickDelayMs(SalarySnapshot(status = SalaryStatus.REST_DAY)))
    }

    @Test fun countdownTicksEverySecondButNotPastTransition() {
        val before = SalarySnapshot(status = SalaryStatus.BEFORE_WORK, nextTransitionMs = 3_600_000)
        assertEquals(1_000L, nextTickDelayMs(before))
        assertEquals(60_000L, nextTickDelayMs(before, RefreshRate.MINUTE))
        assertEquals(500L, nextTickDelayMs(before.copy(nextTransitionMs = 500)))
    }
}
