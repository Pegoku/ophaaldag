package com.pegoku.curem3

import com.pegoku.curem3.data.CureParser
import com.pegoku.curem3.data.ReminderSettings
import com.pegoku.curem3.reminders.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CureParserTest {
    private val raw = javaClass.getResourceAsStream("/postcodecheck_fixture.json")!!.bufferedReader().readText()
    private val data = CureParser.parse(raw, fetchedAt = 0L)

    @Test
    fun parsesAddressInfo() {
        assertEquals("1234AB", data.info.postcode)
        assertEquals("Voorbeeldstraat 1 A", data.info.fullAddress)
        assertEquals("1234 AB Eindhoven", data.info.cityLine)
        assertEquals("eindhoven", data.gemeente)
        assertEquals(51.0, data.info.lat!!, 1e-6)
        assertEquals("https://static.mijnafvalwijzer.nl/logos/eindhoven.png", data.logoUrl)
    }

    @Test
    fun parsesPickupsSortedAndTyped() {
        assertEquals(30, data.pickups.size)
        assertEquals(LocalDate.of(2026, 9, 18), data.pickups.first().localDate)
        assertTrue(data.pickups.zipWithNext().all { (a, b) -> a.date <= b.date })
        assertEquals(setOf("papier", "restafval"), data.pickups.map { it.type }.toSet())
    }

    @Test
    fun labelsComeFromSeparationInfo() {
        assertEquals("Restafval", data.labelFor("restafval"))
        assertEquals("Papier en karton", data.labelFor("papier"))
        assertEquals("unknown", data.labelFor("unknown"))
    }

    @Test
    fun nokSectionsAreEmptyNotCrashing() {
        assertEquals(6, data.containers.size)
        assertTrue(data.wasteAbc.isNotEmpty())
        assertEquals(listOf("restafval"), data.wasteAbc["Restafval"])
        assertEquals(0xFF407235L, data.seedColor)
    }

    @Test
    fun nokTopLevelThrows() {
        val ex = runCatching { CureParser.parse("""{"response":"NOK","data":[],"error":"No Afvaldata"}""") }.exceptionOrNull()
        assertTrue(ex is CureParser.ApiException)
        assertEquals("No Afvaldata", ex!!.message)
    }

    @Test
    fun reminderPlanningPicksEveningBefore() {
        val settings = ReminderSettings(enabled = true, dayBefore = true, hour = 19, minute = 0)
        val planned = ReminderScheduler.nextReminder(data, settings, now = LocalDateTime.of(2026, 9, 19, 12, 0))
        assertNotNull(planned)
        // Next pickup after 19 Sep 12:00 is restafval on 23 Sep -> remind on 22 Sep 19:00.
        assertEquals(LocalDateTime.of(2026, 9, 22, 19, 0), planned!!.fireAt)
        assertEquals(listOf("restafval"), planned.pickups.map { it.type })
    }

    @Test
    fun reminderRespectsTypeFilterAndDisabled() {
        val onlyPaper = ReminderSettings(enabled = true, dayBefore = false, hour = 7, minute = 30, types = setOf("papier"))
        val planned = ReminderScheduler.nextReminder(data, onlyPaper, now = LocalDateTime.of(2026, 9, 19, 12, 0))
        assertEquals(LocalDate.of(2026, 9, 25), planned!!.pickupDate)
        assertEquals(LocalDateTime.of(2026, 9, 25, 7, 30), planned.fireAt)
        assertNull(ReminderScheduler.nextReminder(data, onlyPaper.copy(enabled = false)))
    }
}
