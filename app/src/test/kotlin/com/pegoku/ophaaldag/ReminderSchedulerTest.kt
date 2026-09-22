package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.data.CureParser
import com.pegoku.ophaaldag.data.ReminderSettings
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
    fun sameDayStreamsShareOneReminder() {
        val pickups = data.pickups.take(2).map { it.copy(date = "2026-10-01") } // papier + restafval on one day
        val d = data.copy(pickups = pickups)
        val plan = ReminderScheduler.plan(d, UserSettings(reminders = base))
        assertEquals(1, plan.size)
        assertEquals(LocalDateTime.of(2026, 9, 30, 19, 0), plan[0].fireAt)
        assertEquals(setOf("papier", "restafval"), plan[0].pickups.map { it.type }.toSet())
        val morning = ReminderScheduler.plan(d, UserSettings(reminders = base.copy(dayBefore = false, hour = 6, minute = 30)))
        assertEquals(LocalDateTime.of(2026, 10, 1, 6, 30), morning[0].fireAt)
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
}
