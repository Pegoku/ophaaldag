/*
 * Cure M3 - a Material 3 client for the Cure Afvalbeheer waste calendar.
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
package com.pegoku.curem3.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pegoku.curem3.R
import com.pegoku.curem3.data.CureData
import com.pegoku.curem3.ui.Routes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class MoreItem(val icon: ImageVector, val title: String, val subtitle: String? = null, val onClick: () -> Unit)

@Composable
fun MoreScreen(data: CureData, onNavigate: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val groups = buildList {
        add(buildList {
            add(MoreItem(Icons.Outlined.Campaign, stringResource(R.string.announcements), null) { onNavigate(Routes.ANNOUNCEMENTS) })
            add(MoreItem(Icons.Outlined.History, stringResource(R.string.messages_history), stringResource(R.string.messages_subtitle)) { onNavigate(Routes.MESSAGES) })
            if (data.tips.isNotEmpty()) add(MoreItem(Icons.Outlined.Lightbulb, stringResource(R.string.tips)) { onNavigate(Routes.TIPS) })
        })
        add(listOf(MoreItem(Icons.Outlined.Place, stringResource(R.string.containers_nearby), stringResource(R.string.containers_subtitle)) { onNavigate(Routes.CONTAINERS) }))
        add(buildList {
            data.municipalityPages.forEachIndexed { i, p -> add(MoreItem(Icons.Outlined.Description, p.title) { onNavigate(Routes.page(i)) }) }
            if (!data.moreInfoHtml.isNullOrBlank()) add(MoreItem(Icons.Outlined.Info, stringResource(R.string.about_cure)) { onNavigate(Routes.INFO) })
            if (!data.faqHtml.isNullOrBlank()) add(MoreItem(Icons.Outlined.QuestionAnswer, stringResource(R.string.faq)) { onNavigate(Routes.FAQ) })
        })
        add(listOf(
            MoreItem(Icons.Outlined.Settings, stringResource(R.string.settings)) { onNavigate(Routes.SETTINGS) },
            MoreItem(Icons.Outlined.Code, stringResource(R.string.source_code), "github.com/Pegoku/cure-m3") { uriHandler.openUri("https://github.com/Pegoku/cure-m3") },
        ))
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.more_title)) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            groups.filter { it.isNotEmpty() }.forEach { group ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        group.forEach { item ->
                            ListItem(
                                headlineContent = { Text(item.title) },
                                supportingContent = item.subtitle?.let { { Text(it) } },
                                leadingContent = { Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.clickable { item.onClick() },
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.about_app), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.about_app_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.data_versions, unixDate(data.info.afvaldataVersion), unixDate(data.info.contentVersion)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun unixDate(seconds: String): String {
    val s = seconds.toLongOrNull() ?: return "–"
    return Instant.ofEpochSecond(s).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
}
