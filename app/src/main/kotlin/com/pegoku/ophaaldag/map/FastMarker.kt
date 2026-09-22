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

import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * A [Marker] that reacts on tap-up instead of on confirmed-single-tap.
 *
 * osmdroid's stock Marker hit-tests in `onSingleTapConfirmed`, which the gesture detector only
 * fires once the double-tap window has elapsed: a very noticeable ~300 ms of nothing happening
 * after your finger lifts. Hit-testing in `onSingleTapUp` makes selection feel immediate. The cost
 * is that double-tap-to-zoom centred exactly on a marker also selects it, which is harmless here.
 */
class FastMarker(mapView: MapView) : Marker(mapView) {

    /** Invoked as soon as the tap lands on this marker. */
    var onTap: (() -> Unit)? = null

    override fun onSingleTapUp(event: MotionEvent, mapView: MapView): Boolean {
        if (!isEnabled || !hitTest(event, mapView)) return false
        onTap?.invoke()
        return true
    }

    /** Already handled on tap-up; swallow the confirmed tap so the map overlay does not also react. */
    override fun onSingleTapConfirmed(event: MotionEvent, mapView: MapView): Boolean =
        isEnabled && hitTest(event, mapView)
}
