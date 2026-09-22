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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pegoku.ophaaldag.OphaaldagApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Fires for the armed reminder (or a snoozed one), shows it, and arms the next moment. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = OphaaldagApplication.from(context)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.repository.awaitCache()
                val settings = app.settings.current()
                val data = app.repository.state.value.data
                val date = intent.getStringExtra(ReminderScheduler.EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val types = intent.getStringArrayExtra(ReminderScheduler.EXTRA_TYPES)?.toList()
                if (data != null && date != null && settings.reminders.enabled) {
                    val pickups = ReminderScheduler.pickupsFor(data, settings, date, types)
                    if (pickups.isNotEmpty()) ReminderNotifications.show(context, data, settings.reminders, date, pickups)
                }
                if (intent.action != ReminderScheduler.ACTION_SNOOZE_FIRE) ReminderScheduler.reschedule(context, data, settings)
            } finally {
                pending.finish()
            }
        }
    }
}
