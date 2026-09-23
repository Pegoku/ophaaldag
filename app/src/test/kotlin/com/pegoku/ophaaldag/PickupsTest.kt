package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.data.PickupDay
import com.pegoku.ophaaldag.data.Pickups
import com.pegoku.ophaaldag.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PickupsTest {
    private val noon = LocalTime.of(12, 0)
    private val pickups = listOf(
        PickupDay(type = "restafval", date = "2026-09-21"),
        PickupDay(type = "gft", date = "2026-09-23"),
        PickupDay(type = "papier", date = "2026-09-25"),
    )

    @Test
    fun todayCountsUntilTheCollectionTime() {
        val today = LocalDate.of(2026, 9, 23)
        assertTrue(Pickups.isUpcoming(today, noon, LocalDateTime.of(2026, 9, 23, 7, 30)))
        assertFalse(Pickups.isUpcoming(today, noon, LocalDateTime.of(2026, 9, 23, 12, 0)))
        assertFalse(Pickups.isUpcoming(today, noon, LocalDateTime.of(2026, 9, 23, 14, 0)))
    }

    @Test
    fun upcomingSkipsTodayOnceItHasBeenCollected() {
        val before = Pickups.upcoming(pickups, noon, LocalDateTime.of(2026, 9, 23, 8, 0))
        assertEquals(listOf("gft", "papier"), before.map { it.type })
        val after = Pickups.upcoming(pickups, noon, LocalDateTime.of(2026, 9, 23, 13, 0))
        assertEquals(listOf("papier"), after.map { it.type })
    }

    @Test
    fun undatedAndPastPickupsNeverCount() {
        val odd = pickups + PickupDay(type = "glas", date = "not a date")
        assertEquals(
            listOf("papier"),
            Pickups.upcoming(odd, noon, LocalDateTime.of(2026, 9, 24, 9, 0)).map { it.type },
        )
    }

    @Test
    fun collectionTimeDefaultsToNoon() {
        assertEquals(LocalTime.of(12, 0), UserSettings().collectedBy)
        assertEquals(LocalTime.of(7, 30), UserSettings(collectedByHour = 7, collectedByMinute = 30).collectedBy)
    }
}
