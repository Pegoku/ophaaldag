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

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import com.pegoku.curem3.CureApplication
import com.pegoku.curem3.MainActivity
import com.pegoku.curem3.R
import com.pegoku.curem3.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = CureApplication.from(context)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.repository.awaitCache()
                val settings = app.settings.current()
                val data = app.repository.state.value.data
                val date = intent.getStringExtra(ReminderScheduler.EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (data != null && date != null && settings.reminders.enabled) {
                    val pickups = ReminderScheduler.pickupsFor(data, settings.reminders, date)
                    if (pickups.isNotEmpty()) notify(context, date, pickups.map { data.labelFor(it.type) })
                }
                ReminderScheduler.reschedule(context, data, settings)
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, date: LocalDate, labels: List<String>) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val today = LocalDate.now()
        val whenText = Dates.relativeDay(context, date, today)
        val title = context.getString(R.string.reminder_title, whenText)
        val text = labels.joinToString(", ")
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CureApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.reminder_body, text, whenText.lowercase())))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(date.toEpochDay().toInt(), notification)
    }
}
