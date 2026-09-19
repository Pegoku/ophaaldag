package com.pegoku.curem3.sync

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pegoku.curem3.CureApplication
import com.pegoku.curem3.MainActivity
import com.pegoku.curem3.R
import com.pegoku.curem3.data.CureData
import com.pegoku.curem3.data.PushMessage
import com.pegoku.curem3.data.SettingsRepository
import java.util.concurrent.TimeUnit

/**
 * Periodic background refresh. Replaces the official app's Firebase pushes: after each refresh
 * any service message (`pushData`) newer than the last one seen is shown as a notification.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = CureApplication.from(applicationContext)
        app.repository.awaitCache()
        if (app.settings.current().address == null) return Result.success()
        val result = app.repository.refresh(force = true)
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "cure-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS, 2, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Called after every successful refresh (foreground or background). */
        suspend fun notifyNewServiceMessages(context: Context, settings: SettingsRepository, data: CureData) {
            val current = settings.current()
            val newest = data.pushMessages.maxOfOrNull { it.date } ?: return
            if (current.lastSeenPush.isEmpty()) {
                // First sync: don't replay history.
                settings.setLastSeenPush(newest)
                return
            }
            val fresh = data.pushMessages.filter { it.date > current.lastSeenPush }.sortedBy { it.date }
            if (fresh.isEmpty()) return
            settings.setLastSeenPush(newest)
            if (!current.serviceMessages) return
            fresh.forEach { post(context, it) }
        }

        private fun post(context: Context, message: PushMessage) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val open = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CureApplication.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.service_message_title))
                .setContentText(message.message.trim())
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.message.trim()))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java).notify(message.date.hashCode(), notification)
        }
    }
}
