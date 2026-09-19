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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.DataState
import com.pegoku.ophaaldag.data.PickupDay
import com.pegoku.ophaaldag.ui.components.SectionTitle
import com.pegoku.ophaaldag.ui.components.WasteIcon
import com.pegoku.ophaaldag.ui.components.ageText
import com.pegoku.ophaaldag.util.Dates
import java.time.LocalDate

@Composable
fun HomeScreen(
    data: CureData,
    state: DataState,
    onRefresh: () -> Unit,
    onOpenWaste: (String) -> Unit,
    onOpenAnnouncement: (String) -> Unit,
    onOpenAnnouncements: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val today = LocalDate.now()
    val upcoming = data.pickups.filter { it.localDate?.isBefore(today) == false }
    val nextDate = upcoming.firstOrNull()?.localDate
    val nextGroup = upcoming.filter { it.localDate == nextDate }
    val later = upcoming.filter { it.localDate != nextDate }.take(10)
    val announcements = data.announcements.filter { it.isActive(today) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(data.info.fullAddress.ifBlank { data.info.postcode }) },
                subtitle = { Text(data.info.cityLine) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val refreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            state = refreshState,
            modifier = Modifier.fillMaxSize().padding(padding),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshState,
                    isRefreshing = state.loading,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.error != null) {
                            Icon(Icons.Outlined.CloudOff, null, modifier = Modifier.height(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(if (state.error == "offline") R.string.offline_banner else R.string.showing_cached),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                stringResource(R.string.updated_ago, ageText(context, data.fetchedAt)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    if (nextDate != null) {
                        NextPickupCard(data, nextDate, nextGroup, onOpenWaste)
                    } else {
                        Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text(stringResource(R.string.no_upcoming), modifier = Modifier.padding(20.dp))
                        }
                    }
                }
                if (later.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.coming_up)) }
                    items(later, key = { it.type + it.date }) { day -> PickupRow(data, day, today, onOpenWaste) }
                }
                if (announcements.isNotEmpty()) {
                    item {
                        SectionTitle(stringResource(R.string.announcements), modifier = Modifier.padding(top = 8.dp)) {
                            TextButton(onClick = onOpenAnnouncements) { Text(stringResource(R.string.see_all)) }
                        }
                    }
                    itemsIndexed(announcements.take(3)) { _, a ->
                        Card(
                            onClick = { onOpenAnnouncement(a.id) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(a.title, style = MaterialTheme.typography.titleMedium)
                                if (a.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        a.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextPickupCard(data: CureData, date: LocalDate, pickups: List<PickupDay>, onOpenWaste: (String) -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(stringResource(R.string.next_pickup), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                Dates.inDays(context, date),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(Dates.long(date), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            pickups.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWaste(day.type) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    WasteIcon(day.type, size = 52.dp)
                    Text(data.labelFor(day.type), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
fun PickupRow(data: CureData, day: PickupDay, today: LocalDate, onOpenWaste: (String) -> Unit) {
    val context = LocalContext.current
    val date = day.localDate ?: return
    ListItem(
        headlineContent = { Text(data.labelFor(day.type)) },
        supportingContent = { Text(Dates.long(date)) },
        leadingContent = { WasteIcon(day.type) },
        trailingContent = {
            Text(Dates.inDays(context, date, today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable { onOpenWaste(day.type) }.padding(horizontal = 4.dp),
    )
}
