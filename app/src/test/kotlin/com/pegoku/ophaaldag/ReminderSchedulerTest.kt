package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.data.CureParser
import com.pegoku.ophaaldag.data.ReminderSettings
import com.pegoku.ophaaldag.data.ReminderTime
import com.pegoku.ophaaldag.data.UserSettings
import com.pegoku.ophaaldag.reminders.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderSchedulerTest {
    private val raw = javaClass.getResourceAsStream("/postcodecheck_fixture.json")!!.bufferedReader().readText()
    private val data = CureParser.parse(raw, fetchedAt = 0L)
    private val base = ReminderSettings(enabled = true, dayBefore = true, hour = 19, minute = 0)

    @Test
    fun overrideGivesEachStreamItsOwnMoment() {
        // Paper keeps the 19:00 evening default; restafval fires at 06:30 on the morning itself.
        val settings = UserSettings(reminders = base.copy(overrides = mapOf("restafval" to ReminderTime(dayBefore = false, hour = 6, minute = 30))))
        val plan = ReminderScheduler.plan(data, settings)
        val rest = plan.first { it.pickupDate == LocalDate.of(2026, 9, 23) }
        assertEquals(LocalDateTime.of(2026, 9, 23, 6, 30), rest.fireAt)
        val paper = plan.first { it.pickupDate == LocalDate.of(2026, 9, 25) }
        assertEquals(LocalDateTime.of(2026, 9, 24, 19, 0), paper.fireAt)
        assertTrue(plan.zipWithNext().all { (a, b) -> !a.fireAt.isAfter(b.fireAt) })
    }

    @Test
    fun sameDayDifferentTimesAreSeparateReminders() {
        val pickups = data.pickups.take(2).map { it.copy(date = "2026-10-01") } // papier + restafval on one day
        val d = data.copy(pickups = pickups)
        val settings = UserSettings(reminders = base.copy(overrides = mapOf("restafval" to ReminderTime(false, 6, 30))))
        val plan = ReminderScheduler.plan(d, settings)
        assertEquals(2, plan.size)
        assertEquals(listOf("papier"), plan[0].pickups.map { it.type })
        assertEquals(LocalDateTime.of(2026, 9, 30, 19, 0), plan[0].fireAt)
        assertEquals(listOf("restafval"), plan[1].pickups.map { it.type })
        assertEquals(LocalDateTime.of(2026, 10, 1, 6, 30), plan[1].fireAt)
        // Without an override both streams share one notification.
        assertEquals(1, ReminderScheduler.plan(d, UserSettings(reminders = base)).size)
    }

    @Test
    fun donePickupsAreSkipped() {
        val settings = UserSettings(reminders = base, donePickups = setOf(UserSettings.doneKey("2026-09-23", "restafval")))
        val next = ReminderScheduler.nextReminder(data, settings, now = LocalDateTime.of(2026, 9, 19, 12, 0))
        assertEquals(LocalDate.of(2026, 9, 25), next!!.pickupDate)
        assertTrue(ReminderScheduler.pickupsFor(data, settings, LocalDate.of(2026, 9, 23)).isEmpty())
    }

    @Test
    fun pickupsForNarrowsToRequestedTypes() {
        val pickups = data.pickups.take(2).map { it.copy(date = "2026-10-01") }
        val d = data.copy(pickups = pickups)
        val settings = UserSettings(reminders = base)
        assertEquals(listOf("papier"), ReminderScheduler.pickupsFor(d, settings, LocalDate.of(2026, 10, 1), listOf("papier")).map { it.type })
        assertEquals(2, ReminderScheduler.pickupsFor(d, settings, LocalDate.of(2026, 10, 1)).size)
        assertNull(ReminderScheduler.nextReminder(d, UserSettings(reminders = base.copy(enabled = false))))
    }

    @Test
    fun overrideCodecRoundTrips() {
        val encoded = ReminderSettings.encodeOverride("papier", ReminderTime(false, 6, 5))
        assertEquals("papier|0|6|5", encoded)
        assertEquals("papier" to ReminderTime(false, 6, 5), ReminderSettings.decodeOverride(encoded))
        assertEquals("odd|type" to ReminderTime(true, 19, 0), ReminderSettings.decodeOverride("odd|type|1|19|0"))
        assertNull(ReminderSettings.decodeOverride("papier|1|25|0"))
        assertNull(ReminderSettings.decodeOverride("garbage"))
    }
}
