package com.pegoku.curem3.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pegoku.curem3.data.CureData
import com.pegoku.curem3.data.PickupDay
import com.pegoku.curem3.data.ReminderSettings
import com.pegoku.curem3.data.UserSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Local replacement for the server-side push reminders of the official app.
 * Schedules a single alarm for the next reminder moment; the receiver re-arms it.
 */
object ReminderScheduler {
    const val EXTRA_DATE = "date"
    private const val REQUEST_CODE = 4242

    data class Planned(val fireAt: LocalDateTime, val pickupDate: LocalDate, val pickups: List<PickupDay>)

    fun pickupsFor(data: CureData, settings: ReminderSettings, date: LocalDate): List<PickupDay> =
        data.pickups.filter { it.date == date.toString() && (settings.types.isEmpty() || it.type in settings.types) }

    fun nextReminder(data: CureData, settings: ReminderSettings, now: LocalDateTime = LocalDateTime.now()): Planned? {
        if (!settings.enabled) return null
        val dates = data.pickups.mapNotNull { it.localDate }.distinct().sorted()
        for (date in dates) {
            val pickups = pickupsFor(data, settings, date)
            if (pickups.isEmpty()) continue
            val fireDate = if (settings.dayBefore) date.minusDays(1) else date
            val fireAt = fireDate.atTime(settings.hour, settings.minute)
            if (fireAt.isAfter(now)) return Planned(fireAt, date, pickups)
        }
        return null
    }

    fun reschedule(context: Context, data: CureData?, settings: UserSettings) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, null)
        am.cancel(pi)
        if (data == null) return
        val planned = nextReminder(data, settings.reminders) ?: return
        val triggerAt = planned.fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = pendingIntent(context, planned.pickupDate)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 10 * 60 * 1000L, intent)
        }
    }

    private fun pendingIntent(context: Context, date: LocalDate?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.pegoku.curem3.REMINDER"
            date?.let { putExtra(EXTRA_DATE, it.toString()) }
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
