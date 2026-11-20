// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PaydayTest {
    private val config = SalaryConfig(monthlySalary = 22000.0, workDaysPerMonth = 22.0, startTime = "09:00", endTime = "18:00")

    @Test fun nextPaydayThisMonthOrNext() {
        assertEquals(LocalDate.of(2026, 11, 25), nextPayday(LocalDate.of(2026, 11, 20), 25))
        assertEquals(LocalDate.of(2026, 11, 20), nextPayday(LocalDate.of(2026, 11, 20), 20))
        assertEquals(LocalDate.of(2026, 12, 10), nextPayday(LocalDate.of(2026, 11, 20), 10))
    }

    @Test fun shortMonthsUseLastDay() {
        assertEquals(LocalDate.of(2027, 2, 28), nextPayday(LocalDate.of(2027, 2, 1), 31))
        assertEquals(LocalDate.of(2026, 11, 30), previousPayday(LocalDate.of(2026, 12, 5), 31))
    }

    @Test fun periodEarnedCountsPastWorkdaysPlusToday() {
        // Payday 15th → period starts Mon 2026-11-16. Now: Fri 2026-11-20 13:30 (half day).
        val info = calculatePayday(LocalDateTime.of(2026, 11, 20, 13, 30), config, 15)!!
        assertEquals(LocalDate.of(2026, 11, 16), info.periodStart)
        assertEquals(25L, info.daysUntil) // next payday 2026-12-15
        assertEquals(4 * 1000.0 + 500.0, info.periodEarned, 1e-6)
    }

    @Test fun weekendsAddNothing() {
        // Sun 2026-11-22 evening: Mon–Fri full = 5000
        val info = calculatePayday(LocalDateTime.of(2026, 11, 22, 20, 0), config, 15)!!
        assertEquals(5000.0, info.periodEarned, 1e-6)
    }

    @Test fun paydayTodayIsZeroDays() {
        val info = calculatePayday(LocalDateTime.of(2026, 11, 15, 9, 0), config, 15)!!
        assertTrue(info.isToday)
    }

    @Test fun nightShiftIsNotDoubleCounted() {
        val night = config.copy(startTime = "22:00", endTime = "06:00", workdays = listOf(0, 1, 2, 3, 4, 5, 6))
        // Period starts 2026-11-16. At Wed 18th 02:00 we are in Tue night's shift (half done).
        val info = calculatePayday(LocalDateTime.of(2026, 11, 18, 2, 0), night, 15)!!
        // Mon 16th night complete (1000) + half of Tue 17th night (500); 15th night belongs to old period
        assertEquals(1500.0, info.periodEarned, 1e-6)
    }

    @Test fun unsetPaydayReturnsNull() {
        assertNull(calculatePayday(LocalDateTime.of(2026, 11, 20, 12, 0), config, 0))
    }
}
