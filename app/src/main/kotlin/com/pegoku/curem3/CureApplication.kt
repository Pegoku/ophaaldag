package com.pegoku.curem3

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.pegoku.curem3.data.CureRepository
import com.pegoku.curem3.data.SettingsRepository
import com.pegoku.curem3.reminders.ReminderScheduler
import com.pegoku.curem3.widget.PickupWidget

class CureApplication : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val repository: CureRepository by lazy {
        CureRepository(this, settings, onDataChanged = { data ->
            ReminderScheduler.reschedule(this, data, settings.current())
            runCatching { PickupWidget().updateAll(this) }
        })
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
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
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        fun from(context: Context): CureApplication = context.applicationContext as CureApplication
    }
}
