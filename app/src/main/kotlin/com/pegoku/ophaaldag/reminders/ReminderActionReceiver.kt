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
package com.pegoku.ophaaldag.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pegoku.ophaaldag.OphaaldagApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Handles the "Done" and "Snooze" buttons on a reminder notification. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val date = intent.getStringExtra(ReminderScheduler.EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val types = intent.getStringArrayExtra(ReminderScheduler.EXTRA_TYPES)?.toList().orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        context.getSystemService(NotificationManager::class.java).cancel(notificationId)
        when (intent.action) {
            ACTION_SNOOZE -> ReminderScheduler.snooze(context, date, types)
            ACTION_DONE -> {
                val pending = goAsync()
                val app = OphaaldagApplication.from(context)
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        types.forEach { app.settings.markDone(date.toString(), it) }
                        app.repository.awaitCache()
                        ReminderScheduler.reschedule(context, app.repository.state.value.data, app.settings.current())
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.pegoku.ophaaldag.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.pegoku.ophaaldag.REMINDER_SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
