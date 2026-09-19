/*
 * Cure M3 - a Material 3 client for the Cure Afvalbeheer waste calendar.
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
package com.pegoku.curem3.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pegoku.curem3.CureApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = CureApplication.from(context)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.repository.awaitCache()
                ReminderScheduler.reschedule(context, app.repository.state.value.data, app.settings.current())
            } finally {
                pending.finish()
            }
        }
    }
}
