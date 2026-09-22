package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.data.ReminderSettings
import com.pegoku.ophaaldag.data.SettingsRepository
import com.pegoku.ophaaldag.data.UserSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class SettingsRepositoryTest {
    private val repo = SettingsRepository(RuntimeEnvironment.getApplication())

    @Test
    fun markDoneKeepsTodayAndDropsPastEntries() = runBlocking {
        repo.markDone("2026-09-20", "papier", today = "2026-09-20")
        repo.markDone("2026-09-23", "restafval", today = "2026-09-22")
        val s = repo.current()
        assertTrue(s.isDone("2026-09-23", "restafval"))
        assertFalse(s.isDone("2026-09-20", "papier"))
        assertFalse(s.isDone("2026-09-23", "papier"))
        assertEquals(setOf(UserSettings.doneKey("2026-09-23", "restafval")), s.donePickups)
    }

    @Test
    fun reminderSettingsRoundTripThroughDataStore() = runBlocking {
        val r = ReminderSettings(
            enabled = true, dayBefore = false, hour = 6, minute = 45, types = setOf("gft"),
            alarmStyle = true, dateChanges = false,
        )
        repo.setReminders(r)
        assertEquals(r, repo.current().reminders)
    }
}
