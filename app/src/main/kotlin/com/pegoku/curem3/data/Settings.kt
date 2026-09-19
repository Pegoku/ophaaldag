package com.pegoku.curem3.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ReminderSettings(
    val enabled: Boolean = false,
    /** true = evening before the pickup, false = morning of the pickup. */
    val dayBefore: Boolean = true,
    val hour: Int = 19,
    val minute: Int = 0,
    /** Empty = all waste types. */
    val types: Set<String> = emptySet(),
)

data class UserSettings(
    val address: Address? = null,
    /** "auto", "nl" or "en". */
    val language: String = "auto",
    val dynamicColor: Boolean = true,
    val reminders: ReminderSettings = ReminderSettings(),
) {
    fun apiLanguage(): String = when (language) {
        "nl", "en" -> language
        else -> if (Locale.getDefault().language == "nl") "nl" else "en"
    }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val postcode = stringPreferencesKey("postcode")
        val houseNumber = stringPreferencesKey("house_number")
        val suffix = stringPreferencesKey("suffix")
        val street = stringPreferencesKey("street")
        val language = stringPreferencesKey("language")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val remEnabled = booleanPreferencesKey("rem_enabled")
        val remDayBefore = booleanPreferencesKey("rem_day_before")
        val remHour = intPreferencesKey("rem_hour")
        val remMinute = intPreferencesKey("rem_minute")
        val remTypes = stringSetPreferencesKey("rem_types")
    }

    val settings: Flow<UserSettings> = context.settingsStore.data.map { p -> p.toSettings() }

    suspend fun current(): UserSettings = settings.first()

    private fun Preferences.toSettings(): UserSettings {
        val postcode = this[Keys.postcode]
        val house = this[Keys.houseNumber]
        val address = if (!postcode.isNullOrBlank() && !house.isNullOrBlank()) {
            Address(postcode, house, this[Keys.suffix] ?: "", this[Keys.street] ?: "")
        } else null
        return UserSettings(
            address = address,
            language = this[Keys.language] ?: "auto",
            dynamicColor = this[Keys.dynamicColor] ?: true,
            reminders = ReminderSettings(
                enabled = this[Keys.remEnabled] ?: false,
                dayBefore = this[Keys.remDayBefore] ?: true,
                hour = this[Keys.remHour] ?: 19,
                minute = this[Keys.remMinute] ?: 0,
                types = this[Keys.remTypes] ?: emptySet(),
            ),
        )
    }

    suspend fun setAddress(address: Address?) {
        context.settingsStore.edit { p ->
            if (address == null) {
                p.remove(Keys.postcode); p.remove(Keys.houseNumber); p.remove(Keys.suffix); p.remove(Keys.street)
            } else {
                p[Keys.postcode] = address.postcode
                p[Keys.houseNumber] = address.houseNumber
                p[Keys.suffix] = address.suffix
                p[Keys.street] = address.street
            }
        }
    }

    suspend fun setLanguage(language: String) = context.settingsStore.edit { it[Keys.language] = language }
    suspend fun setDynamicColor(enabled: Boolean) = context.settingsStore.edit { it[Keys.dynamicColor] = enabled }

    suspend fun setReminders(r: ReminderSettings) {
        context.settingsStore.edit { p ->
            p[Keys.remEnabled] = r.enabled
            p[Keys.remDayBefore] = r.dayBefore
            p[Keys.remHour] = r.hour
            p[Keys.remMinute] = r.minute
            p[Keys.remTypes] = r.types
        }
    }
}
