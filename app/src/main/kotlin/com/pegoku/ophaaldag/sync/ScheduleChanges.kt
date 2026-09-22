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
package com.pegoku.ophaaldag.sync

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
import com.pegoku.ophaaldag.data.PickupChange
import com.pegoku.ophaaldag.data.PickupDiff
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tells the user when a refresh shows that Cure moved, cancelled or added a pickup. The official
 * app has nothing like it: holiday shifts are exactly when people miss a collection.
 */
object ScheduleChanges {
    private const val NOTIFICATION_ID = 7001

    fun notify(context: Context, previous: CureData, data: CureData, today: LocalDate = LocalDate.now()) {
        if (previous.info.postcode != data.info.postcode || previous.info.huisnummer != data.info.huisnummer) return
        val changes = PickupDiff.compute(previous.pickups, data.pickups, today)
        if (changes.isEmpty()) return
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val lines = changes.map { describe(context, data, it) }
        val open = PendingIntent.getActivity(
            context, 2, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = context.resources.getQuantityString(R.plurals.schedule_change_title, changes.size, changes.size)
        val style = NotificationCompat.InboxStyle().also { s -> lines.forEach { s.addLine(it) } }
        val notification = NotificationCompat.Builder(context, OphaaldagApplication.CHANNEL_SCHEDULE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    fun describe(context: Context, data: CureData, change: PickupChange): String {
        val label = data.labelFor(change.type)
        return when (change) {
            is PickupChange.Moved -> context.getString(R.string.change_moved, label, day(change.from), day(change.to))
            is PickupChange.Cancelled -> context.getString(R.string.change_cancelled, label, day(change.date))
            is PickupChange.Added -> context.getString(R.string.change_added, label, day(change.date))
        }
    }

    private fun day(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
}
