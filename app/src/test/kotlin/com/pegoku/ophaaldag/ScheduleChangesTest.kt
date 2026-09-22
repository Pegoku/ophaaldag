package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.data.CureParser
import com.pegoku.ophaaldag.data.PickupChange
import com.pegoku.ophaaldag.sync.ScheduleChanges
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class, qualifiers = "en")
class ScheduleChangesTest {
    private val raw = javaClass.getResourceAsStream("/postcodecheck_fixture.json")!!.bufferedReader().readText()
    private val data = CureParser.parse(raw, fetchedAt = 0L)

    @Test
    fun describesChangesWithStreamLabels() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(
            "Papier en karton moved from Thu 24 Sep to Fri 25 Sep",
            ScheduleChanges.describe(context, data, PickupChange.Moved("papier", LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25))),
        )
        assertEquals(
            "Restafval on Wed 23 Sep is cancelled",
            ScheduleChanges.describe(context, data, PickupChange.Cancelled("restafval", LocalDate.of(2026, 9, 23))),
        )
        assertEquals(
            "Extra pickup: Restafval on Thu 1 Oct",
            ScheduleChanges.describe(context, data, PickupChange.Added("restafval", LocalDate.of(2026, 10, 1))),
        )
    }
}
