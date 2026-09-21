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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.WasteTypes
import com.pegoku.ophaaldag.map.ContainerCluster
import com.pegoku.ophaaldag.map.FastMarker
import com.pegoku.ophaaldag.map.MarkerIcons
import com.pegoku.ophaaldag.map.PdokTiles
import com.pegoku.ophaaldag.map.clusterContainers
import com.pegoku.ophaaldag.ui.components.DetailTopBar
import com.pegoku.ophaaldag.ui.components.WasteIcon
import com.pegoku.ophaaldag.util.Geo
import com.pegoku.ophaaldag.util.MapShare
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.events.MapEventsReceiver
import kotlin.math.roundToInt

/** Radius, in dp on screen, within which containers collapse into one cluster pin. */
private const val CLUSTER_RADIUS_DP = 22.0

private const val INITIAL_ZOOM = 15

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
    var selected by remember { mutableStateOf<ContainerCluster?>(null) }
    var zoom by remember { mutableIntStateOf(INITIAL_ZOOM) }

    val home = remember(data) {
        data.info.lat?.let { lat -> data.info.lon?.let { lon -> GeoPoint(lat, lon) } }
    }
    val types = remember(data) { data.containers.map { it.wasteType }.distinct().sorted() }
    val shown = remember(data, filter) {
        val lat = data.info.lat
        val lon = data.info.lon
        data.containers
            .filter { filter.isBlank() || it.wasteType == filter }
            .sortedBy { c ->
                if (lat != null && lon != null) Geo.distanceMeters(lat, lon, c.lat!!, c.lon!!) else 0.0
            }
            .take(80)
    }

    // Re-clustered per integer zoom step: the pins have a fixed size in dp, so how much ground they
    // cover — and therefore what overlaps — changes every time the user zooms.
    val clusters = remember(shown, zoom) {
        val latitude = home?.latitude ?: shown.firstOrNull()?.lat ?: 52.0
        clusterContainers(shown, CLUSTER_RADIUS_DP * Geo.metersPerDp(latitude, zoom))
    }

    val title = stringResource(R.string.containers_nearby)
    val noMapApp = stringResource(R.string.no_map_app)
    val homeColor = MaterialTheme.colorScheme.primary.toArgb()

    val mapView = remember {
        PdokTiles.configure(context)
        MapView(context).apply {
            setTileSource(PdokTiles.source)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            isTilesScaledToDpi = true
            // Pinch-zoom only; the stock zoom buttons fade in over the detail card and look nothing
            // like the rest of the app.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(INITIAL_ZOOM.toDouble())
            home?.let { controller.setCenter(it) }
        }
    }
    MapLifecycle(mapView)

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean = false

            override fun onZoom(event: ZoomEvent?): Boolean {
                zoom = mapView.zoomLevelDouble.roundToInt()
                return false
            }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    // Markers are built once per cluster set. Selection is handled separately below, because
    // rebuilding eighty overlays on every tap is what made the old version feel sluggish.
    val markers = remember(clusters) { mutableMapOf<ContainerCluster, FastMarker>() }
    DisposableEffect(mapView, clusters, homeColor) {
        markers.clear()
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
                FastMarker(mapView).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = MarkerIcons.home(context, homeColor)
                    infoWindow = null
                },
            )
        }

        clusters.forEach { cluster ->
            val marker = FastMarker(mapView).apply {
                position = GeoPoint(cluster.latitude, cluster.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = MarkerIcons.cluster(context, cluster.colorsArgb(), cluster.size)
                infoWindow = null
                onTap = { selected = cluster }
            }
            markers[cluster] = marker
            mapView.overlays.add(marker)
        }
        // Zooming re-clusters, and the pin behind the open card may no longer exist afterwards.
        if (selected != null && selected !in markers) selected = null
        if (clusters.isNotEmpty() && home == null) {
            mapView.controller.setCenter(GeoPoint(clusters.first().latitude, clusters.first().longitude))
        }
        mapView.invalidate()
        onDispose { }
    }

    // Swapping two cached icons and moving one overlay to the end of the list, so the selected pin
    // draws on top of its neighbours instead of under them.
    LaunchedEffect(selected, markers) {
        markers.forEach { (cluster, marker) ->
            marker.icon = MarkerIcons.cluster(context, cluster.colorsArgb(), cluster.size, cluster == selected)
        }
        selected?.let { markers[it] }?.let { marker ->
            mapView.overlays.remove(marker)
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapChip(stringResource(R.string.all_types), filter.isBlank(), null) { filter = ""; selected = null }
                types.forEach { t ->
                    MapChip(data.labelFor(t), filter == t, WasteTypes.style(t).color) { filter = t; selected = null }
                }
            }

            // The attribution is a licence condition, so it stays put at the bottom edge and the
            // detail card is stacked above it rather than over it.
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                AnimatedVisibility(
                    visible = selected != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    ClusterCard(data, selected, onDirections = { cluster ->
                        val uri = "geo:${cluster.latitude},${cluster.longitude}" +
                            "?q=${cluster.latitude},${cluster.longitude}" +
                            "(${android.net.Uri.encode(cluster.label(data))})"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri())) }
                    })
                }
                Text(
                    PdokTiles.ATTRIBUTION,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Detail for the tapped pin. Takes a nullable cluster and holds the last non-null one so the exit
 * animation still has something to draw while it slides away.
 */
@Composable
private fun ClusterCard(data: CureData, cluster: ContainerCluster?, onDirections: (ContainerCluster) -> Unit) {
    var last by remember { mutableStateOf(cluster) }
    if (cluster != null) last = cluster
    val shownCluster = cluster ?: last ?: return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                shownCluster.types.take(3).forEach { WasteIcon(it, size = 36.dp) }
            }
            Column(Modifier.weight(1f)) {
                Text(shownCluster.label(data), style = MaterialTheme.typography.titleMedium)
                Text(
                    shownCluster.sharedAddress ?: stringResource(R.string.containers_here, shownCluster.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = { onDirections(shownCluster) }) {
                Icon(Icons.Outlined.Directions, contentDescription = stringResource(R.string.open_in_maps))
            }
        }
    }
}

/** Filter chips double as the map legend, so each one is outlined in its stream's pin colour. */
@Composable
private fun MapChip(label: String, checked: Boolean, color: Color?, onClick: () -> Unit) {
    FilterChip(
        selected = checked,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        elevation = FilterChipDefaults.filterChipElevation(elevation = 3.dp),
        border = color?.let { BorderStroke(if (checked) 2.dp else 1.5.dp, it) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            // Blended rather than alpha-tinted: these sit over map tiles, and a translucent
            // container lets streets through and washes the label out.
            selectedContainerColor = color
                ?.let { lerp(MaterialTheme.colorScheme.surface, it, 0.30f) }
                ?: MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/** A cluster's streams, in the fixed order the pin's wedges are drawn. */
private fun ContainerCluster.colorsArgb(): List<Int> =
    types.map { WasteTypes.style(it).color.toArgb() }

/** "Glass" for one stream, "Glass · Household waste" for several. */
private fun ContainerCluster.label(data: CureData): String =
    types.joinToString(" · ") { data.labelFor(it) }

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
