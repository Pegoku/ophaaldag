package com.pegoku.curem3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pegoku.curem3.R
import com.pegoku.curem3.data.CureData
import com.pegoku.curem3.data.WasteTypes
import com.pegoku.curem3.util.Dates
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(data: CureData, onOpenWaste: (String) -> Unit) {
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var filter by rememberSaveable { mutableStateOf("") }
    var selectedDay by rememberSaveable { mutableStateOf<String?>(null) }
    val month = YearMonth.parse(monthKey)
    val today = LocalDate.now()
    val types = data.pickups.map { it.type }.distinct()

    val inMonth = data.pickups.filter { d ->
        val ld = d.localDate ?: return@filter false
        YearMonth.from(ld) == month && (filter.isBlank() || d.type == filter)
    }
    val byDate = inMonth.groupBy { it.localDate!! }
    val listed = selectedDay?.let { key -> byDate[LocalDate.parse(key)] ?: emptyList() } ?: inMonth

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_calendar)) }) },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(onClick = { monthKey = month.minusMonths(1).toString(); selectedDay = null }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.previous_month))
                    }
                    Text(
                        Dates.monthYear(month.atDay(1)),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clickable { monthKey = YearMonth.now().toString(); selectedDay = null },
                    )
                    FilledTonalIconButton(onClick = { monthKey = month.plusMonths(1).toString(); selectedDay = null }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.next_month))
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    ToggleButton(checked = filter.isBlank(), onCheckedChange = { filter = "" }) {
                        Text(stringResource(R.string.all_types))
                    }
                    types.forEach { type ->
                        ToggleButton(checked = filter == type, onCheckedChange = { filter = if (it) type else "" }) {
                            Text(data.labelFor(type), maxLines = 1)
                        }
                    }
                }
            }
            item {
                MonthGrid(
                    month = month,
                    today = today,
                    selected = selectedDay?.let { LocalDate.parse(it) },
                    typesByDate = byDate.mapValues { e -> e.value.map { it.type } },
                    onSelect = { d -> selectedDay = if (selectedDay == d.toString()) null else d.toString() },
                )
            }
            if (listed.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_pickups_month),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(listed, key = { it.type + it.date }) { day -> PickupRow(data, day, today, onOpenWaste) }
            }
            if (selectedDay != null) {
                item {
                    TextButton(onClick = { selectedDay = null }, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(stringResource(R.string.this_month))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    typesByDate: Map<LocalDate, List<String>>,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val days = month.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dow ->
                Text(
                    dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..days) {
                            val date = month.atDay(dayNum)
                            val types = typesByDate[date] ?: emptyList()
                            val isToday = date == today
                            val isSelected = date == selected
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                    )
                                    .clickable { onSelect(date) },
                            ) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        dayNum.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isToday -> MaterialTheme.colorScheme.onPrimary
                                            date.isBefore(today) -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 3.dp)) {
                                    types.take(3).forEach { t ->
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(WasteTypes.style(t).color))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

