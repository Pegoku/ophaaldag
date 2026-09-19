package com.pegoku.curem3.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.pegoku.curem3.R
import com.pegoku.curem3.data.CureData
import com.pegoku.curem3.data.ReminderSettings
import com.pegoku.curem3.data.UserSettings
import com.pegoku.curem3.reminders.ReminderScheduler
import com.pegoku.curem3.ui.AppViewModel
import com.pegoku.curem3.ui.components.DetailTopBar
import com.pegoku.curem3.ui.components.SectionTitle
import com.pegoku.curem3.ui.components.WasteIcon
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, settings: UserSettings, data: CureData, onBack: () -> Unit, onChangeAddress: () -> Unit) {
    val context = LocalContext.current
    val reminders = settings.reminders
    var showTimePicker by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted
    }
    val types = data.pickups.map { it.type }.distinct()

    fun update(block: ReminderSettings.() -> ReminderSettings) = vm.setReminders(reminders.block())

    Scaffold(topBar = { DetailTopBar(stringResource(R.string.settings), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SectionTitle(stringResource(R.string.address))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(data.info.fullAddress, style = MaterialTheme.typography.titleMedium)
                        Text(data.info.cityLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = onChangeAddress) { Text(stringResource(R.string.change_address)) }
                }
            }

            SectionTitle(stringResource(R.string.reminders), modifier = Modifier.padding(top = 12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reminders_enable)) },
                    trailingContent = {
                        Switch(
                            checked = reminders.enabled,
                            onCheckedChange = { on ->
                                if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsEnabled) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                update { copy(enabled = on) }
                            },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable { update { copy(enabled = !enabled) } },
                )
                if (reminders.enabled) {
                    if (!notificationsEnabled) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(stringResource(R.string.notifications_denied), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                )
                            }) { Text(stringResource(R.string.open_system_settings)) }
                        }
                    }
                    Text(stringResource(R.string.reminder_when), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        ToggleButton(checked = reminders.dayBefore, onCheckedChange = { update { copy(dayBefore = true, hour = if (hour < 12) 19 else hour) } }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.evening_before), maxLines = 1)
                        }
                        ToggleButton(checked = !reminders.dayBefore, onCheckedChange = { update { copy(dayBefore = false, hour = if (hour >= 12) 7 else hour) } }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.morning_of), maxLines = 1)
                        }
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.reminder_time)) },
                        trailingContent = {
                            Text("%02d:%02d".format(reminders.hour, reminders.minute), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clickable { showTimePicker = true },
                    )
                    Text(stringResource(R.string.reminder_types), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { t ->
                            val selected = reminders.types.isEmpty() || t in reminders.types
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    update {
                                        val all = types.toSet()
                                        val current = if (this.types.isEmpty()) all else this.types
                                        val next = if (t in current) current - t else current + t
                                        copy(types = if (next == all || next.isEmpty()) emptySet() else next)
                                    }
                                },
                                label = { Text(data.labelFor(t)) },
                                leadingIcon = { WasteIcon(t, size = 18.dp) },
                            )
                        }
                    }
                    val planned = remember(reminders, data) { ReminderScheduler.nextReminder(data, reminders) }
                    Text(
                        planned?.let {
                            stringResource(R.string.next_reminder, it.fireAt.format(DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.getDefault())))
                        } ?: stringResource(R.string.no_reminder_planned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            SectionTitle(stringResource(R.string.appearance), modifier = Modifier.padding(top = 12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.dynamic_color)) },
                        supportingContent = { Text(stringResource(R.string.dynamic_color_desc)) },
                        trailingContent = { Switch(checked = settings.dynamicColor, onCheckedChange = { vm.setDynamicColor(it) }) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clickable { vm.setDynamicColor(!settings.dynamicColor) },
                    )
                }
                Text(stringResource(R.string.language), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    listOf("auto" to R.string.language_auto, "nl" to R.string.language_nl, "en" to R.string.language_en).forEach { (code, label) ->
                        ToggleButton(checked = settings.language == code, onCheckedChange = { if (it) vm.setLanguage(code) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(label), maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = reminders.hour, initialMinute = reminders.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { update { copy(hour = state.hour, minute = state.minute) }; showTimePicker = false }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.reminder_time)) },
        ) {
            TimePicker(state = state)
        }
    }
}

