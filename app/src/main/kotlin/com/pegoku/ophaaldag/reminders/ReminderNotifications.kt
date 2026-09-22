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

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import com.pegoku.ophaaldag.MainActivity
import com.pegoku.ophaaldag.OphaaldagApplication
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.PickupDay
import com.pegoku.ophaaldag.data.ReminderSettings
import com.pegoku.ophaaldag.util.Dates
import java.time.LocalDate

/** Builds the pickup reminder notification with its Done and Snooze actions. */
object ReminderNotifications {
    fun show(context: Context, data: CureData, reminders: ReminderSettings, date: LocalDate, pickups: List<PickupDay>) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val types = pickups.map { it.type }.distinct().sorted()
        val id = notificationId(date, types)
        val whenText = Dates.relativeDay(context, date, LocalDate.now())
        val text = types.joinToString(", ") { data.labelFor(it) }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val channel = if (reminders.alarmStyle) OphaaldagApplication.CHANNEL_ALARM else OphaaldagApplication.CHANNEL_REMINDERS
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title, whenText))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.reminder_body, text, whenText.lowercase())))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.reminder_done), action(context, ReminderActionReceiver.ACTION_DONE, id, date, types))
            .addAction(0, context.getString(R.string.reminder_snooze), action(context, ReminderActionReceiver.ACTION_SNOOZE, id, date, types))
        if (reminders.alarmStyle) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            // Insistent: the alarm sound loops until the notification is dismissed or acted on.
            builder.setDefaults(0)
            val notification = builder.build()
            notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
            context.getSystemService(NotificationManager::class.java).notify(id, notification)
            return
        }
        context.getSystemService(NotificationManager::class.java).notify(id, builder.build())
    }

    /** Stable per (date, types) so a paper reminder at 19:00 does not replace a GFT one at 06:30. */
    fun notificationId(date: LocalDate, types: List<String>): Int =
        (date.toString() + types.sorted().joinToString(",")).hashCode() and 0x3fffffff or 0x100000

    private fun action(context: Context, action: String, id: Int, date: LocalDate, types: List<String>): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_DATE, date.toString())
            putExtra(ReminderScheduler.EXTRA_TYPES, types.toTypedArray())
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, id)
        }
        val requestCode = id * 2 + (if (action == ReminderActionReceiver.ACTION_DONE) 0 else 1)
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
