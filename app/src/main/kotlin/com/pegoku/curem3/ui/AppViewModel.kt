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
package com.pegoku.curem3.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pegoku.curem3.CureApplication
import com.pegoku.curem3.calendar.CalendarSync
import com.pegoku.curem3.calendar.DeviceCalendar
import com.pegoku.curem3.data.Address
import com.pegoku.curem3.data.CureApi
import com.pegoku.curem3.data.CureParser
import com.pegoku.curem3.data.DataState
import com.pegoku.curem3.data.ReminderSettings
import com.pegoku.curem3.data.UserSettings
import com.pegoku.curem3.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import java.io.IOException

sealed interface AddressResult {
    data object Success : AddressResult
    data object UnknownPostcode : AddressResult
    data object NoData : AddressResult
    data object Offline : AddressResult
    data class Error(val message: String) : AddressResult
}

class AppViewModel(private val app: CureApplication) : ViewModel() {
    private val api = CureApi()

    val settings: StateFlow<UserSettings?> =
        app.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val data: StateFlow<DataState> = app.repository.state

    init {
        viewModelScope.launch {
            app.repository.awaitCache()
            if (app.settings.current().address != null) app.repository.refresh()
        }
    }

    fun refreshIfStale() {
        viewModelScope.launch {
            app.repository.awaitCache()
            if (app.settings.current().address != null) app.repository.refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch { app.repository.refresh(force = true) }
    }

    suspend fun lookupStreets(postcode: String): List<String> =
        try {
            api.streetList(postcode)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }

    suspend fun submitAddress(address: Address): AddressResult = viewModelScope.async {
        try {
            when (api.tinyCheck(address)) {
                CureApi.TinyResult.UNKNOWN_POSTCODE -> return@async AddressResult.UnknownPostcode
                CureApi.TinyResult.NO_DATA -> return@async AddressResult.NoData
                CureApi.TinyResult.OK -> Unit
            }
            val result = app.repository.changeAddress(address)
            result.fold(
                onSuccess = { AddressResult.Success },
                onFailure = { e ->
                    when (e) {
                        is IOException -> AddressResult.Offline
                        is CureParser.ApiException -> AddressResult.NoData
                        else -> AddressResult.Error(e.message ?: e.javaClass.simpleName)
                    }
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AddressResult.Offline
        } catch (e: Exception) {
            AddressResult.Error(e.message ?: e.javaClass.simpleName)
        }
    }.await()

    fun clearAddress() {
        viewModelScope.launch { app.repository.clearAddress() }
    }

    fun setReminders(reminders: ReminderSettings) {
        viewModelScope.launch {
            app.settings.setReminders(reminders)
            val current = app.settings.current()
            ReminderScheduler.reschedule(app, data.value.data, current)
            val d = data.value.data
            if (d != null && current.calendarId != null) CalendarSync.sync(app, current.calendarId, d, reminders)
        }
    }

    fun setServiceMessages(enabled: Boolean) {
        viewModelScope.launch { app.settings.setServiceMessages(enabled) }
    }

    suspend fun writableCalendars(): List<DeviceCalendar> = CalendarSync.writableCalendars(app)

    /** Enables mirroring into [calendar] and performs the first sync. Returns inserted event count. */
    suspend fun enableCalendar(calendar: DeviceCalendar): Int {
        app.settings.setCalendar(calendar.id, calendar.name)
        val d = data.value.data ?: return 0
        return CalendarSync.sync(app, calendar.id, d, app.settings.current().reminders)
    }

    suspend fun disableCalendar() {
        app.settings.current().calendarId?.let { CalendarSync.remove(app, it) }
        app.settings.setCalendar(null, "")
    }

    suspend fun shareIcs() {
        val d = data.value.data ?: return
        CalendarSync.shareIcs(app, d, app.settings.current().reminders)
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { app.settings.setDynamicColor(enabled) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            app.settings.setLanguage(language)
            app.repository.refresh(force = true)
        }
    }

    class Factory(private val app: CureApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(app) as T
    }
}
