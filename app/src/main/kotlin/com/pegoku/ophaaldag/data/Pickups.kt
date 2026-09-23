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
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Cure publishes pickup dates without a time, so the app assumes the round is finished at the
 * `collectedBy` moment the user set. Everything that shows "the next pickup" filters through here,
 * so today's date stops being the next one as soon as that moment passes.
 */
object Pickups {
    fun isUpcoming(date: LocalDate, collectedBy: LocalTime, now: LocalDateTime = LocalDateTime.now()): Boolean =
        now.isBefore(date.atTime(collectedBy))

    fun upcoming(
        pickups: List<PickupDay>,
        collectedBy: LocalTime,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<PickupDay> = pickups.filter { p -> p.localDate?.let { isUpcoming(it, collectedBy, now) } == true }
}
