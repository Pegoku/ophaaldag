package com.pegoku.curem3.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pegoku.curem3.R
import com.pegoku.curem3.data.CureData
import com.pegoku.curem3.ui.components.SectionTitle
import com.pegoku.curem3.ui.components.WasteIcon

@Composable
fun GuideScreen(data: CureData, onOpenWaste: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()
    val results = remember(trimmed, data) {
        if (trimmed.length < 2) emptyList() else data.wasteAbc.entries
            .filter { it.key.contains(trimmed, ignoreCase = true) }
            .sortedWith(compareBy({ !it.key.startsWith(trimmed, ignoreCase = true) }, { it.key.lowercase() }))
            .take(150)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.guide_title)) }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.guide_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear)) }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (trimmed.length < 2) {
                    item { SectionTitle(stringResource(R.string.waste_streams)) }
                    items(data.separation, key = { it.iconName }) { info ->
                        ListItem(
                            headlineContent = { Text(info.afvalTitle) },
                            leadingContent = { WasteIcon(info.iconName) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                            modifier = Modifier.clickable { onOpenWaste(info.iconName) },
                        )
                    }
                } else if (results.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.guide_no_results, trimmed),
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    item { SectionTitle(stringResource(R.string.guide_results, results.size)) }
                    items(results, key = { it.key }) { (item, types) ->
                        ListItem(
                            headlineContent = { Text(item.replaceFirstChar { it.uppercase() }) },
                            supportingContent = {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    types.forEach { t ->
                                        AssistChip(
                                            onClick = { onOpenWaste(t) },
                                            label = { Text(data.labelFor(t)) },
                                            leadingIcon = { WasteIcon(t, size = 18.dp) },
                                        )
                                    }
                                }
                            },
                            leadingContent = { WasteIcon(types.first(), size = 40.dp, modifier = Modifier.size(40.dp)) },
                        )
                    }
                }
            }
        }
    }
}
