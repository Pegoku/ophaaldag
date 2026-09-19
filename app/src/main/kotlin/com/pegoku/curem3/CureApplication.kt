package com.pegoku.curem3

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.pegoku.curem3.calendar.CalendarSync
import com.pegoku.curem3.data.CureRepository
import com.pegoku.curem3.data.SettingsRepository
import com.pegoku.curem3.reminders.ReminderScheduler
import com.pegoku.curem3.sync.SyncWorker
import com.pegoku.curem3.widget.PickupWidget

class CureApplication : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val repository: CureRepository by lazy {
        CureRepository(this, settings, onDataChanged = { data ->
            val current = settings.current()
            ReminderScheduler.reschedule(this, data, current)
            runCatching { PickupWidget().updateAll(this) }
            if (data != null) {
                runCatching { SyncWorker.notifyNewServiceMessages(this, settings, data) }
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
                CHANNEL_SERVICE,
                getString(R.string.channel_service),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = getString(R.string.channel_service_desc) },
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_SERVICE = "service"
        fun from(context: Context): CureApplication = context.applicationContext as CureApplication
    }
}
