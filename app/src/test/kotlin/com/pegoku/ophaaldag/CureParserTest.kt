/*
 * Ophaaldag - a Material 3 client for the Cure Afvalbeheer waste calendar.
 * Copyright (C) 2026 Pere Gomila
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.data.CureParser
import com.pegoku.ophaaldag.data.ReminderSettings
import com.pegoku.ophaaldag.reminders.ReminderScheduler
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
    fun includesNextYearDatesWhenServicePublishesThem() {
        val future = """{"response":"OK","data":{"ophaaldagen":{"response":"OK","data":[{"type":"papier","date":"2026-12-25"}]},"ophaaldagenNext":{"response":"OK","data":[{"type":"papier","date":"2027-01-08"},{"type":"papier","date":"2026-12-25"}]}}}"""
        assertEquals(listOf("2026-12-25", "2027-01-08"), CureParser.parse(future).pickups.map { it.date })
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
