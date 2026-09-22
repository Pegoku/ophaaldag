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
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import com.pegoku.ophaaldag.MainActivity
import com.pegoku.ophaaldag.OphaaldagApplication
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.PickupDay
import com.pegoku.ophaaldag.data.ReminderSettings
import com.pegoku.ophaaldag.data.WasteTypes
import com.pegoku.ophaaldag.util.Dates
import java.time.LocalDate

/**
 * Builds the pickup reminder notification: the stream's colour and a coloured badge, a title that
 * leads with the day, a body that says what to do, and Done / Snooze actions. Tapping the body opens
 * the app but keeps the notification; only Done or a swipe removes it.
 */
object ReminderNotifications {
    fun show(context: Context, data: CureData, reminders: ReminderSettings, date: LocalDate, pickups: List<PickupDay>) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val types = pickups.map { it.type }.distinct().sorted()
        val id = notificationId(date, types)
        val today = LocalDate.now()
        val whenText = Dates.relativeDay(context, date, today)
        val labels = types.joinToString(", ") { data.labelFor(it) }
        val body = context.getString(
            if (date.isAfter(today)) R.string.reminder_body_evening else R.string.reminder_body_morning,
            Dates.long(date),
        )
        val color = WasteTypes.style(types.first()).color.toArgb()
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val channel = if (reminders.alarmStyle) OphaaldagApplication.CHANNEL_ALARM else OphaaldagApplication.CHANNEL_REMINDERS
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(badge(context, color))
            .setColor(color)
            .setContentTitle(context.getString(R.string.reminder_title, whenText, labels))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (reminders.alarmStyle) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.reminder_done), action(context, ReminderActionReceiver.ACTION_DONE, id, date, types))
            .addAction(0, context.getString(R.string.reminder_snooze), action(context, ReminderActionReceiver.ACTION_SNOOZE, id, date, types))
        val notification = builder.build()
        // Insistent: the alarm sound loops until the notification is dismissed or acted on.
        if (reminders.alarmStyle) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    /** Stable per (date, types) so two reminders on the same day do not replace each other. */
    fun notificationId(date: LocalDate, types: List<String>): Int =
        (date.toString() + types.sorted().joinToString(",")).hashCode() and 0x3fffffff or 0x100000

    /** A filled circle in the stream colour with the white bin glyph, used as the large icon. */
    private fun badge(context: Context, color: Int): Bitmap {
        val size = (64 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        ResourcesCompat.getDrawable(context.resources, R.drawable.ic_notification, context.theme)?.let { glyph ->
            val inset = (size * 0.22f).toInt()
            glyph.setBounds(inset, inset, size - inset, size - inset)
            glyph.setTint(android.graphics.Color.WHITE)
            glyph.draw(canvas)
        }
        return bitmap
    }

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
