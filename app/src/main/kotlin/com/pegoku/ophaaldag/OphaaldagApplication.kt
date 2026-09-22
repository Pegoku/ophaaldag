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
package com.pegoku.ophaaldag

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.glance.appwidget.updateAll
import com.pegoku.ophaaldag.calendar.CalendarSync
import com.pegoku.ophaaldag.data.CureRepository
import com.pegoku.ophaaldag.data.SettingsRepository
import com.pegoku.ophaaldag.reminders.ReminderScheduler
import com.pegoku.ophaaldag.sync.ScheduleChanges
import com.pegoku.ophaaldag.sync.SyncWorker
import com.pegoku.ophaaldag.widget.PickupWidget

class OphaaldagApplication : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val repository: CureRepository by lazy {
        CureRepository(this, settings, onDataChanged = { previous, data ->
            val current = settings.current()
            ReminderScheduler.reschedule(this, data, current)
            runCatching { PickupWidget().updateAll(this) }
            if (data != null) {
                runCatching { SyncWorker.notifyNewServiceMessages(this, settings, data) }
                if (previous != null && current.reminders.dateChanges) {
                    runCatching { ScheduleChanges.notify(this, previous, data) }
                }
                current.calendarId?.let { id -> runCatching { CalendarSync.sync(this, id, data, current.reminders) } }
            }
        })
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        SyncWorker.schedule(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = getString(R.string.channel_reminders_desc) },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM,
                getString(R.string.channel_alarm),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.channel_alarm_desc)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SCHEDULE,
                getString(R.string.channel_schedule),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = getString(R.string.channel_schedule_desc) },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                getString(R.string.channel_service),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = getString(R.string.channel_service_desc) },
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_SERVICE = "service"
        const val CHANNEL_ALARM = "reminders_alarm"
        const val CHANNEL_SCHEDULE = "schedule"
        fun from(context: Context): OphaaldagApplication = context.applicationContext as OphaaldagApplication
    }
}
