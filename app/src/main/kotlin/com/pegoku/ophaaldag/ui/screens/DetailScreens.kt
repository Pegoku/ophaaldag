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
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.ContainerLocation
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.ui.components.DetailTopBar
import com.pegoku.ophaaldag.ui.components.HtmlText
import com.pegoku.ophaaldag.ui.components.WasteIcon
import com.pegoku.ophaaldag.util.Dates
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun WasteDetailScreen(data: CureData, type: String, onBack: () -> Unit) {
    val info = data.separationFor(type)
    val today = LocalDate.now()
    val next = data.pickups.filter { it.type == type && it.localDate?.isBefore(today) == false }.take(6)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(data.labelFor(type), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { BackIcon(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WasteIcon(type, size = 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (next.isNotEmpty()) {
                        Text(stringResource(R.string.next_pickup), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Dates.long(next.first().localDate!!), style = MaterialTheme.typography.titleLarge)
                        Text(Dates.inDays(context, next.first().localDate!!, today), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(stringResource(R.string.no_upcoming), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (next.size > 1) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    next.drop(1).forEach { d -> SuggestionChip(onClick = {}, label = { Text(Dates.short(d.localDate!!)) }) }
                }
            }
            if (info != null && info.text.isNotBlank()) {
                HtmlText(info.text, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BackIcon(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}

@Composable
fun AnnouncementsScreen(data: CureData, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val today = LocalDate.now()
    val list = data.announcements.sortedBy { !it.isActive(today) }
    Scaffold(topBar = { DetailTopBar(stringResource(R.string.announcements), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(list) { _, a ->
                ListItem(
                    headlineContent = { Text(a.title) },
                    supportingContent = { Text(a.description.ifBlank { a.text.replace(Regex("<[^>]+>"), "").trim() }, maxLines = 2) },
                    overlineContent = { Text(a.date) },
                    modifier = Modifier.clickable { onOpen(a.id) },
                )
            }
        }
    }
}

@Composable
fun AnnouncementDetailScreen(data: CureData, id: String, onBack: () -> Unit) {
    val a = data.announcements.firstOrNull { it.id == id }
    Scaffold(topBar = { DetailTopBar(stringResource(R.string.announcements), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (a == null) return@Column
            val bitmap = remember(a.topImage) {
                a.topImage.takeIf { it.startsWith("data:image") }?.substringAfter(",", "")?.let { b64 ->
                    runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).padding(bottom = 16.dp),
                )
            }
            Text(a.title, style = MaterialTheme.typography.headlineSmall)
            Text(a.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            HtmlText(a.text)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun MessagesScreen(data: CureData, onBack: () -> Unit) {
    Scaffold(topBar = { DetailTopBar(stringResource(R.string.messages_history), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(data.pushMessages) { _, m ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(m.date.take(16), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(m.message.trim(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun TipsScreen(data: CureData, onBack: () -> Unit) {
    Scaffold(topBar = { DetailTopBar(stringResource(R.string.tips), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(data.tips) { _, t ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.extraLarge) {
                    Column(Modifier.padding(20.dp)) {
                        Text(t.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        HtmlText(t.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HtmlPageScreen(title: String, html: String, onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeFlexibleTopAppBar(title = { Text(title, style = MaterialTheme.typography.headlineMedium) }, navigationIcon = { BackIcon(onBack) }, scrollBehavior = scrollBehavior) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            HtmlText(html)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ContainersScreen(data: CureData, onBack: () -> Unit) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf("") }
    val lat = data.info.lat
    val lon = data.info.lon
    val types = data.containers.map { it.wasteType }.distinct().sorted()
    val sorted = remember(data, filter) {
        data.containers
            .filter { filter.isBlank() || it.wasteType == filter }
            .map { c -> c to (if (lat != null && lon != null) distanceMeters(lat, lon, c.lat!!, c.lon!!) else Double.NaN) }
            .sortedBy { if (it.second.isNaN()) Double.MAX_VALUE else it.second }
            .take(80)
    }
    Scaffold(topBar = { DetailTopBar(stringResource(R.string.containers_nearby), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (data.containers.isEmpty()) {
                item {
                    Text(stringResource(R.string.no_containers), Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
                }
                return@LazyColumn
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    ToggleButton(checked = filter.isBlank(), onCheckedChange = { filter = "" }) { Text(stringResource(R.string.all_types)) }
                    types.forEach { t ->
                        ToggleButton(checked = filter == t, onCheckedChange = { filter = if (it) t else "" }) { Text(data.labelFor(t), maxLines = 1) }
                    }
                }
            }
            itemsIndexed(sorted) { _, (c, dist) ->
                ContainerRow(data, c, dist) {
                    val uri = "geo:${c.latitude},${c.longitude}?q=${c.latitude},${c.longitude}(${Uri.encode(c.address)})".toUri()
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                }
            }
        }
    }
}

private object Uri {
    fun encode(s: String): String = android.net.Uri.encode(s)
}

@Composable
private fun ContainerRow(data: CureData, c: ContainerLocation, dist: Double, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(c.address) },
        supportingContent = { Text(listOf(data.labelFor(c.wasteType), c.city).filter { it.isNotBlank() }.joinToString(" · ")) },
        leadingContent = { WasteIcon(c.wasteType) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (!dist.isNaN()) {
                    Text(
                        if (dist < 1000) stringResource(R.string.distance_m, dist.roundToInt()) else stringResource(R.string.distance_km, dist / 1000),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Icon(Icons.Outlined.Directions, contentDescription = stringResource(R.string.open_in_maps), tint = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * atan2(sqrt(a), sqrt(1 - a))
}

