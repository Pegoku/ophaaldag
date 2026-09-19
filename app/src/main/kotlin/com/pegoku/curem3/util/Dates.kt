package com.pegoku.curem3.util

import android.content.Context
import com.pegoku.curem3.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object Dates {
    fun relativeDay(context: Context, date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days == 0L -> context.getString(R.string.today)
            days == 1L -> context.getString(R.string.tomorrow)
            days in 2..6 -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }
            else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
        }
    }

    fun inDays(context: Context, date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, date).toInt()
        return when {
            days <= 0 -> context.getString(R.string.today)
            days == 1 -> context.getString(R.string.tomorrow)
            else -> context.resources.getQuantityString(R.plurals.in_days, days, days)
        }
    }

    fun long(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())).replaceFirstChar { it.uppercase() }

    fun monthYear(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())).replaceFirstChar { it.uppercase() }

    fun short(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}
