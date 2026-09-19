package com.pegoku.curem3

import com.pegoku.curem3.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class, qualifiers = "en")
class DatesTest {
    @Test
    fun september18IsPastOnSeptember19() {
        val context = RuntimeEnvironment.getApplication()
        val today = LocalDate.of(2026, 9, 19)
        assertEquals("1 day ago", Dates.inDays(context, today.minusDays(1), today))
        assertEquals(context.getString(R.string.today), Dates.inDays(context, today, today))
        assertEquals(context.getString(R.string.tomorrow), Dates.inDays(context, today.plusDays(1), today))
        assertEquals("In 6 days", Dates.inDays(context, today.plusDays(6), today))
    }
}
