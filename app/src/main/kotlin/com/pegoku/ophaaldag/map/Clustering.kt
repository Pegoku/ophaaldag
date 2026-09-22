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
package com.pegoku.ophaaldag.map

import com.pegoku.ophaaldag.data.ContainerLocation
import com.pegoku.ophaaldag.util.Geo

/**
 * One map pin. Cure puts several containers at the same address (four household waste plus a
 * glass bank on one square is normal), so at anything but the closest zoom they are drawn as a
 * single marker that names every stream underneath it.
 */
data class ContainerCluster(
    val latitude: Double,
    val longitude: Double,
    val items: List<ContainerLocation>,
) {
    /** Distinct streams under this pin, in the order they were encountered. */
    val types: List<String> = items.map { it.wasteType }.distinct()

    val size: Int get() = items.size

    /** The address shared by every container here, or null when the cluster spans several. */
    val sharedAddress: String? =
        items.map { it.address }.distinct().singleOrNull()?.takeIf { it.isNotBlank() }
}

/**
 * Greedy proximity clustering: walk the list in order, and for each container not yet taken, pull
 * in every remaining one within [radiusMeters].
 *
 * The input is expected to be sorted nearest-first, which makes the greedy pass deterministic and
 * biases cluster centres towards the containers the user is most likely to want.
 */
fun clusterContainers(items: List<ContainerLocation>, radiusMeters: Double): List<ContainerCluster> {
    val taken = BooleanArray(items.size)
    val clusters = ArrayList<ContainerCluster>()
    for (i in items.indices) {
        if (taken[i]) continue
        val seed = items[i]
        val seedLat = seed.lat ?: continue
        val seedLon = seed.lon ?: continue
        taken[i] = true
        val group = ArrayList<ContainerLocation>()
        group.add(seed)
        if (radiusMeters > 0) {
            for (j in i + 1 until items.size) {
                if (taken[j]) continue
                val other = items[j]
                val lat = other.lat ?: continue
                val lon = other.lon ?: continue
                if (Geo.distanceMeters(seedLat, seedLon, lat, lon) <= radiusMeters) {
                    taken[j] = true
                    group.add(other)
                }
            }
        }
        clusters.add(
            ContainerCluster(
                latitude = group.sumOf { it.lat!! } / group.size,
                longitude = group.sumOf { it.lon!! } / group.size,
                items = group,
            ),
        )
    }
    return clusters
}
