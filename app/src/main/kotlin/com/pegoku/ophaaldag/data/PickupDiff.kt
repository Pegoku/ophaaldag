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
package com.pegoku.ophaaldag.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** A pickup that differs between two versions of the same address's calendar. */
sealed interface PickupChange {
    val type: String

    /** The pickup that used to be on [from] is now on [to]; holiday shifts look like this. */
    data class Moved(override val type: String, val from: LocalDate, val to: LocalDate) : PickupChange

    /** A pickup that disappeared without a replacement nearby. */
    data class Cancelled(override val type: String, val date: LocalDate) : PickupChange

    /** A pickup that appeared inside the range the previous document already covered. */
    data class Added(override val type: String, val date: LocalDate) : PickupChange
}

/**
 * Compares two pickup lists for the same address and reports what changed from [today] on.
 *
 * Dates that only extend the horizon (a new year being published) are not changes and are
 * ignored; so is a document that simply covers less of the future than the previous one.
 */
object PickupDiff {
    /** A removed date pairs with an added one of the same type this close to it. */
    const val MOVE_WINDOW_DAYS = 7L

    fun compute(old: List<PickupDay>, new: List<PickupDay>, today: LocalDate): List<PickupChange> {
        val oldByType = futureDates(old, today)
        val newByType = futureDates(new, today)
        val changes = mutableListOf<PickupChange>()
        for (type in oldByType.keys.intersect(newByType.keys)) {
            val oldDates = oldByType.getValue(type)
            val newDates = newByType.getValue(type)
            val oldMax = oldDates.max()
            val newMax = newDates.max()
            val removed = (oldDates - newDates).sorted().toMutableList()
            val added = (newDates - oldDates).sorted().toMutableList()
            for (from in removed.toList()) {
                val to = added.minByOrNull { abs(ChronoUnit.DAYS.between(from, it)) }
                    ?.takeIf { abs(ChronoUnit.DAYS.between(from, it)) <= MOVE_WINDOW_DAYS } ?: continue
                added.remove(to); removed.remove(from)
                changes += PickupChange.Moved(type, from, to)
            }
            // Leftovers beyond the other document's horizon are coverage differences, not changes.
            removed.filter { !it.isAfter(newMax) }.forEach { changes += PickupChange.Cancelled(type, it) }
            added.filter { !it.isAfter(oldMax) }.forEach { changes += PickupChange.Added(type, it) }
        }
        return changes.sortedBy { it.sortDate() }
    }

    private fun futureDates(pickups: List<PickupDay>, today: LocalDate): Map<String, Set<LocalDate>> =
        pickups.mapNotNull { p -> p.localDate?.takeIf { !it.isBefore(today) }?.let { p.type to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
            .filterValues { it.isNotEmpty() }

    private fun PickupChange.sortDate(): LocalDate = when (this) {
        is PickupChange.Moved -> from
        is PickupChange.Cancelled -> date
        is PickupChange.Added -> date
    }
}
