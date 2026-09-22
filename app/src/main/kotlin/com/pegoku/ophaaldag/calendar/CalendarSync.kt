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
package com.pegoku.ophaaldag.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.ReminderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class DeviceCalendar(val id: Long, val name: String, val account: String)

/** Mirrors the pickup calendar into a device calendar (CalendarContract) or an .ics file. */
object CalendarSync {
    private const val TAG_PREFIX = "ophaaldag:"

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun writableCalendars(context: Context): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val list = mutableListOf<DeviceCalendar>()
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getInt(3) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    list += DeviceCalendar(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "")
                }
            }
        }
        list
    }

    /** Replaces all our events in [calendarId] with the current pickups. Returns inserted count. */
    suspend fun sync(context: Context, calendarId: Long, data: CureData, reminders: ReminderSettings): Int = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext 0
        remove(context, calendarId)
        val resolver = context.contentResolver
        var count = 0
        val today = LocalDate.now()
        data.pickups.filter { it.localDate?.isBefore(today.minusDays(1)) == false }.forEach { day ->
            val date = day.localDate ?: return@forEach
            // Calendar alarms only count minutes before the event, so a morning-of reminder has no
            // calendar equivalent; those streams get the event without an alarm.
            val time = reminders.timeFor(day.type)
            val alarm = reminders.enabled && reminders.includes(day.type) && time.dayBefore
            val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, context.getString(R.string.calendar_event_title, data.labelFor(day.type)))
                put(CalendarContract.Events.DESCRIPTION, "$TAG_PREFIX${day.type}:${day.date}")
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, start + 24 * 60 * 60 * 1000L)
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                put(CalendarContract.Events.HAS_ALARM, if (alarm) 1 else 0)
            }
            val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return@forEach
            count++
            if (alarm) {
                val minutesBeforeMidnight = 24 * 60 - (time.hour * 60 + time.minute)
                resolver.insert(
                    CalendarContract.Reminders.CONTENT_URI,
                    ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(uri))
                        put(CalendarContract.Reminders.MINUTES, minutesBeforeMidnight)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    },
                )
            }
        }
        count
    }

    suspend fun remove(context: Context, calendarId: Long): Int = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext 0
        context.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf(calendarId.toString(), "$TAG_PREFIX%"),
        )
    }

    fun buildIcs(context: Context, data: CureData, reminders: ReminderSettings): String {
        val fmt = DateTimeFormatter.BASIC_ISO_DATE
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ophaaldag//Afvalkalender//NL\r\nCALSCALE:GREGORIAN\r\n")
        sb.append("X-WR-CALNAME:").append(escape(context.getString(R.string.calendar_name, data.info.fullAddress))).append("\r\n")
        data.pickups.forEach { day ->
            val date = day.localDate ?: return@forEach
            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:").append(day.type).append('-').append(day.date).append("@ophaaldag\r\n")
            sb.append("DTSTAMP:").append(fmt.format(LocalDate.now())).append("T000000Z\r\n")
            sb.append("DTSTART;VALUE=DATE:").append(fmt.format(date)).append("\r\n")
            sb.append("DTEND;VALUE=DATE:").append(fmt.format(date.plusDays(1))).append("\r\n")
            sb.append("SUMMARY:").append(escape(context.getString(R.string.calendar_event_title, data.labelFor(day.type)))).append("\r\n")
            sb.append("TRANSP:TRANSPARENT\r\n")
            val time = reminders.timeFor(day.type)
            if (reminders.enabled && reminders.includes(day.type) && time.dayBefore) {
                val minutes = 24 * 60 - (time.hour * 60 + time.minute)
                sb.append("BEGIN:VALARM\r\nACTION:DISPLAY\r\nDESCRIPTION:Reminder\r\nTRIGGER:-PT").append(minutes).append("M\r\nEND:VALARM\r\n")
            }
            sb.append("END:VEVENT\r\n")
        }
        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    /** Writes the .ics to cache and opens a chooser to import or share it. */
    suspend fun shareIcs(context: Context, data: CureData, reminders: ReminderSettings) {
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, "ics").apply { mkdirs() }.resolve("afvalkalender.ics").also { it.writeText(buildIcs(context, data, reminders)) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/calendar").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val send = Intent(Intent.ACTION_SEND).setType("text/calendar").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val intent = if (view.resolveActivity(context.packageManager) != null) view else send
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.calendar_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")
}
