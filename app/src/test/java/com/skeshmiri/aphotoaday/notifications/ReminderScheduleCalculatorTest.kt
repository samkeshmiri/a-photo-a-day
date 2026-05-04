package com.skeshmiri.aphotoaday.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduleCalculatorTest {
    private val zoneId = ZoneId.of("Europe/London")

    @Test
    fun `triggerForDay stays within the daytime reminder window`() {
        val trigger = ReminderScheduleCalculator.triggerForDay(
            date = LocalDate.of(2026, 3, 29),
            zoneId = zoneId,
        )

        assertEquals(LocalDate.of(2026, 3, 29), trigger.toLocalDate())
        assertTrue(!trigger.toLocalTime().isBefore(LocalTime.of(9, 0)))
        assertTrue(trigger.toLocalTime().isBefore(LocalTime.of(18, 0)))
    }

    @Test
    fun `nextTriggerAt keeps the reminder on the same day when the window is still open`() {
        val now = ZonedDateTime.of(2026, 3, 29, 8, 30, 0, 0, zoneId)

        val trigger = ReminderScheduleCalculator.nextTriggerAt(
            now = now,
            lastHandledDate = null,
        )

        assertEquals(LocalDate.of(2026, 3, 29), trigger.toLocalDate())
    }

    @Test
    fun `nextTriggerAt moves to tomorrow after todays reminder was already handled`() {
        val today = LocalDate.of(2026, 3, 29)
        val now = ZonedDateTime.of(2026, 3, 29, 10, 0, 0, 0, zoneId)

        val trigger = ReminderScheduleCalculator.nextTriggerAt(
            now = now,
            lastHandledDate = today,
        )

        assertEquals(LocalDate.of(2026, 3, 30), trigger.toLocalDate())
    }

    @Test
    fun `nextTriggerAt moves to tomorrow after the daily window ends`() {
        val now = ZonedDateTime.of(2026, 3, 29, 18, 5, 0, 0, zoneId)

        val trigger = ReminderScheduleCalculator.nextTriggerAt(
            now = now,
            lastHandledDate = null,
        )

        assertEquals(LocalDate.of(2026, 3, 30), trigger.toLocalDate())
    }
}
