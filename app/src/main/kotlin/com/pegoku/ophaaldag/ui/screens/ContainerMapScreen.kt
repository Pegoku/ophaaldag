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
package com.pegoku.ophaaldag.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.ContainerLocation
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.WasteTypes
import com.pegoku.ophaaldag.map.MarkerIcons
import com.pegoku.ophaaldag.map.PdokTiles
import com.pegoku.ophaaldag.ui.components.DetailTopBar
import com.pegoku.ophaaldag.ui.components.WasteIcon
import com.pegoku.ophaaldag.util.MapShare
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Every nearby container on one map, coloured by waste stream.
 *
 * This lives in-app rather than in the phone's map app on purpose: Android has no intent that
 * hands a set of custom pins to Google Maps, so an external map can only ever show one container
 * at a time. The KML share in the top bar covers the map apps that *can* import a whole list.
 */
@Composable
fun ContainerMapScreen(data: CureData, initialFilter: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(initialFilter) }
    var selected by remember { mutableStateOf<ContainerLocation?>(null) }

    val home = remember(data) { data.info.lat?.let { lat -> data.info.lon?.let { lon -> GeoPoint(lat, lon) } } }
    val types = remember(data) { data.containers.map { it.wasteType }.distinct().sorted() }
    val shown = remember(data, filter) {
        val lat = data.info.lat
        val lon = data.info.lon
        data.containers
            .filter { filter.isBlank() || it.wasteType == filter }
            .sortedBy { c -> if (lat != null && lon != null) distanceMeters(lat, lon, c.lat!!, c.lon!!) else 0.0 }
            .take(80)
    }

    val title = stringResource(R.string.containers_nearby)
    val noMapApp = stringResource(R.string.no_map_app)

    val mapView = remember {
        PdokTiles.configure(context)
        MapView(context).apply {
            setTileSource(PdokTiles.source)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            // Pinch-zoom only; the stock zoom buttons fade in over the detail card and look nothing
            // like the rest of the app.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(15.0)
            home?.let { controller.setCenter(it) }
        }
    }
    MapLifecycle(mapView)

    val homeColor = MaterialTheme.colorScheme.primary.toArgb()

    // Rebuilt whenever the filter or the selection changes; 80 markers is small enough that a full
    // rebuild is cheaper and less error-prone than diffing the overlay list.
    DisposableEffect(mapView, shown, selected) {
        mapView.overlays.clear()

        // Added first so it is consulted last: a tap that hits no marker clears the detail card.
        mapView.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    selected = null
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }),
        )

        home?.let { point ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = MarkerIcons.home(context, homeColor)
                    infoWindow = null
                    setOnMarkerClickListener { _, _ -> true }
                },
            )
        }

        shown.forEach { c ->
            val color = WasteTypes.style(c.wasteType).color.toArgb()
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(c.lat!!, c.lon!!)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = MarkerIcons.dot(context, color, selected = c === selected)
                    infoWindow = null
                    relatedObject = c
                    setOnMarkerClickListener { marker, _ ->
                        selected = marker.relatedObject as? ContainerLocation
                        true
                    }
                },
            )
        }
        if (shown.isNotEmpty() && home == null) {
            mapView.controller.setCenter(GeoPoint(shown.first().lat!!, shown.first().lon!!))
        }
        mapView.invalidate()
        onDispose { }
    }

    Scaffold(
        topBar = {
            DetailTopBar(title, onBack) {
                IconButton(onClick = {
                    val places = shown.map { it.toMapPlace(data, context, Double.NaN) }
                    scope.launch {
                        if (!MapShare.sharePlaces(context, title, places)) {
                            Toast.makeText(context, noMapApp, Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Icon(Icons.Outlined.IosShare, contentDescription = stringResource(R.string.containers_export_kml))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                MapChip(stringResource(R.string.all_types), filter.isBlank()) { filter = ""; selected = null }
                types.forEach { t ->
                    MapChip(data.labelFor(t), filter == t) { filter = t; selected = null }
                }
            }

            Text(
                PdokTiles.ATTRIBUTION,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (selected != null) 96.dp else 8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )

            selected?.let { c ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        WasteIcon(c.wasteType)
                        Column(Modifier.weight(1f)) {
                            Text(data.labelFor(c.wasteType), style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(c.address, c.city).filter { it.isNotBlank() }.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalIconButton(onClick = {
                            val uri = "geo:${c.latitude},${c.longitude}?q=${c.latitude},${c.longitude}(${android.net.Uri.encode(c.address)})".toUri()
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }) {
                            Icon(Icons.Outlined.Directions, contentDescription = stringResource(R.string.open_in_maps))
                        }
                    }
                }
            }
        }
    }
}

/** Opaque and slightly raised: these float over map tiles, so the default translucency reads badly. */
@Composable
private fun MapChip(label: String, checked: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = checked,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        elevation = FilterChipDefaults.filterChipElevation(elevation = 3.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

/** osmdroid's MapView keeps its own tile threads; they have to follow the host lifecycle. */
@Composable
private fun MapLifecycle(mapView: MapView) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
}
