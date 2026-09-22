package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.data.PickupChange
import com.pegoku.ophaaldag.data.PickupDay
import com.pegoku.ophaaldag.data.PickupDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PickupDiffTest {
    private val today = LocalDate.of(2026, 9, 22)

    private fun days(type: String, vararg dates: String) = dates.map { PickupDay(nameType = type, type = type, date = it) }

    @Test
    fun holidayShiftIsReportedAsMove() {
        val old = days("papier", "2026-09-25", "2026-10-09", "2026-10-23")
        val new = days("papier", "2026-09-26", "2026-10-09", "2026-10-23")
        assertEquals(
            listOf(PickupChange.Moved("papier", LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26))),
            PickupDiff.compute(old, new, today),
        )
    }

    @Test
    fun farApartDatesAreCancelledAndAdded() {
        val old = days("gft", "2026-09-25", "2026-11-20")
        val new = days("gft", "2026-10-20", "2026-11-20")
        assertEquals(
            listOf(PickupChange.Cancelled("gft", LocalDate.of(2026, 9, 25)), PickupChange.Added("gft", LocalDate.of(2026, 10, 20))),
            PickupDiff.compute(old, new, today),
        )
    }

    @Test
    fun horizonExtensionIsNotAChange() {
        val old = days("restafval", "2026-12-16", "2026-12-30")
        val new = days("restafval", "2026-12-16", "2026-12-30", "2027-01-13", "2027-01-27")
        assertTrue(PickupDiff.compute(old, new, today).isEmpty())
        // The reverse (a shorter document) is not a wave of cancellations either.
        assertTrue(PickupDiff.compute(new, old, today).isEmpty())
    }

    @Test
    fun pastDatesAndNewStreamsAreIgnored() {
        val old = days("papier", "2026-09-18", "2026-09-25")
        val new = days("papier", "2026-09-25") + days("pmd", "2026-09-30")
        assertTrue(PickupDiff.compute(old, new, today).isEmpty())
    }

    @Test
    fun identicalListsProduceNothing() {
        val list = days("papier", "2026-09-25", "2026-10-09") + days("gft", "2026-09-24")
        assertTrue(PickupDiff.compute(list, list, today).isEmpty())
    }

    @Test
    fun movesPairNearestAndSortByDate() {
        val old = days("gft", "2026-10-01", "2026-10-15") + days("papier", "2026-09-24")
        val new = days("gft", "2026-10-02", "2026-10-16") + days("papier", "2026-09-23")
        val changes = PickupDiff.compute(old, new, today)
        assertEquals(3, changes.size)
        assertEquals(PickupChange.Moved("papier", LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 23)), changes[0])
        assertEquals(PickupChange.Moved("gft", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)), changes[1])
        assertEquals(PickupChange.Moved("gft", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 16)), changes[2])
    }
}
