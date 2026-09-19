package com.pegoku.curem3.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pegoku.curem3.CureApplication
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
        runCatching { api.streetList(postcode) }.getOrDefault(emptyList())

    suspend fun submitAddress(address: Address): AddressResult {
        return try {
            when (api.tinyCheck(address)) {
                CureApi.TinyResult.UNKNOWN_POSTCODE -> return AddressResult.UnknownPostcode
                CureApi.TinyResult.NO_DATA -> return AddressResult.NoData
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
        } catch (e: IOException) {
            AddressResult.Offline
        } catch (e: Exception) {
            AddressResult.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    fun clearAddress() {
        viewModelScope.launch { app.repository.clearAddress() }
    }

    fun setReminders(reminders: ReminderSettings) {
        viewModelScope.launch {
            app.settings.setReminders(reminders)
            ReminderScheduler.reschedule(app, data.value.data, app.settings.current())
        }
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
