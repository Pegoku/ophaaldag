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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.PickupDay
import com.pegoku.ophaaldag.data.ReminderSettings
import com.pegoku.ophaaldag.data.UserSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Local replacement for the server-side push reminders of the official app.
 *
 * Every waste type has its own reminder moment (the global default unless overridden), so one
 * pickup day can produce several alarms: paper the evening before, GFT the same morning. Only the
 * next moment is armed; the receiver re-arms after each one. Pickups the user marked as done are
 * skipped, and a snoozed reminder lives on its own alarm so it survives a reschedule.
 */
object ReminderScheduler {
    const val ACTION_FIRE = "com.pegoku.ophaaldag.REMINDER"
    const val ACTION_SNOOZE_FIRE = "com.pegoku.ophaaldag.REMINDER_SNOOZED"
    const val EXTRA_DATE = "date"
    const val EXTRA_TYPES = "types"
    const val SNOOZE_MS = 60 * 60 * 1000L
    private const val REQUEST_CODE = 4242
    private const val REQUEST_CODE_SNOOZE = 4243

    data class Planned(val fireAt: LocalDateTime, val pickupDate: LocalDate, val pickups: List<PickupDay>)

    /** Pickups on [date] that reminders cover, optionally narrowed to [types]. Done pickups are excluded. */
    fun pickupsFor(
        data: CureData,
        settings: UserSettings,
        date: LocalDate,
        types: Collection<String>? = null,
    ): List<PickupDay> = data.pickups.filter {
        it.date == date.toString() && settings.reminders.includes(it.type) &&
            (types == null || it.type in types) && !settings.isDone(it.date, it.type)
    }

    /** Every (fire moment, pickup date, pickups) group the current data yields, earliest first. */
    fun plan(data: CureData, settings: UserSettings): List<Planned> {
        val r = settings.reminders
        if (!r.enabled) return emptyList()
        return data.pickups
            .filter { r.includes(it.type) && !settings.isDone(it.date, it.type) }
            .mapNotNull { p -> p.localDate?.let { Triple(fireAt(it, r.timeFor(p.type)), it, p) } }
            .groupBy { it.first to it.second }
            .map { (key, group) -> Planned(key.first, key.second, group.map { it.third }) }
            .sortedBy { it.fireAt }
    }

    fun nextReminder(data: CureData, settings: UserSettings, now: LocalDateTime = LocalDateTime.now()): Planned? =
        plan(data, settings).firstOrNull { it.fireAt.isAfter(now) }

    private fun fireAt(pickup: LocalDate, time: com.pegoku.ophaaldag.data.ReminderTime): LocalDateTime =
        (if (time.dayBefore) pickup.minusDays(1) else pickup).atTime(time.hour, time.minute)

    fun reschedule(context: Context, data: CureData?, settings: UserSettings) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, null, emptyList())
        am.cancel(pi)
        if (data == null) return
        val planned = nextReminder(data, settings) ?: return
        val triggerAt = planned.fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = pendingIntent(context, planned.pickupDate, planned.pickups.map { it.type })
        // Bin reminders do not require exact-alarm access.
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 10 * 60 * 1000L, intent)
    }

    /** Re-shows the reminder for [types] on [date] about an hour from now. */
    fun snooze(context: Context, date: LocalDate, types: List<String>) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_FIRE
            putExtra(EXTRA_DATE, date.toString())
            putExtra(EXTRA_TYPES, types.toTypedArray())
        }
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE_SNOOZE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        context.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + SNOOZE_MS, 5 * 60 * 1000L, pi)
    }

    private fun pendingIntent(context: Context, date: LocalDate?, types: List<String>): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            date?.let { putExtra(EXTRA_DATE, it.toString()) }
            if (types.isNotEmpty()) putExtra(EXTRA_TYPES, types.toTypedArray())
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
