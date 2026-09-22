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
package com.pegoku.ophaaldag.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate

data class DataState(
    val data: CureData? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val cacheLoaded: Boolean = false,
)

/**
 * Single source of truth for the pickup document. Keeps the last successful raw
 * response on disk so the app opens instantly and works offline.
 */
class CureRepository(
    private val context: Context,
    val settings: SettingsRepository,
    private val api: CureApi = CureApi(),
    /** Called with the document that was replaced (null on the first load) and the new one. */
    private val onDataChanged: suspend (previous: CureData?, data: CureData?) -> Unit = { _, _ -> },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cacheFile get() = File(context.filesDir, "postcodecheck.json")
    private val refreshMutex = Mutex()

    private val _state = MutableStateFlow(DataState())
    val state: StateFlow<DataState> = _state

    init {
        scope.launch { loadCache() }
    }

    private suspend fun loadCache() {
        val data = withContext(Dispatchers.IO) {
            runCatching {
                if (cacheFile.exists()) CureParser.parse(cacheFile.readText(), cacheFile.lastModified()) else null
            }.getOrNull()
        }
        _state.update { it.copy(data = data, cacheLoaded = true) }
    }

    suspend fun awaitCache() {
        state.first { it.cacheLoaded }
    }

    /** Refreshes from the network when there is an address and the cache is stale (or [force]). */
    suspend fun refresh(force: Boolean = false): Result<CureData> = refreshMutex.withLock {
        val s = settings.current()
        val address = s.address ?: return Result.failure(IllegalStateException("No address"))
        val current = _state.value.data
        if (!force && current != null && System.currentTimeMillis() - current.fetchedAt < STALE_AFTER_MS) {
            return Result.success(current)
        }
        _state.update { it.copy(loading = true, error = null) }
        return try {
            val raw = api.postcodeCheck(address, s.apiLanguage())
            val parsed = CureParser.parse(raw)
            withContext(Dispatchers.IO) { cacheFile.writeText(raw) }
            _state.update { it.copy(data = parsed, loading = false, error = null) }
            onDataChanged(current, parsed)
            Result.success(parsed)
        } catch (e: CancellationException) {
            _state.update { it.copy(loading = false) }
            throw e
        } catch (e: Exception) {
            val message = when (e) {
                is CureParser.ApiException -> e.message ?: "API error"
                is IOException -> "offline"
                else -> e.message ?: e.javaClass.simpleName
            }
            _state.update { it.copy(loading = false, error = message) }
            Result.failure(e)
        }
    }

    /** Replaces the address, drops the cache and loads the new document. */
    suspend fun changeAddress(address: Address): Result<CureData> {
        settings.setAddress(address)
        withContext(Dispatchers.IO) { cacheFile.delete() }
        _state.update { DataState(cacheLoaded = true, loading = true) }
        return refresh(force = true)
    }

    suspend fun clearAddress() {
        settings.setAddress(null)
        withContext(Dispatchers.IO) { cacheFile.delete() }
        _state.update { DataState(cacheLoaded = true) }
        onDataChanged(null, null)
    }

    fun upcoming(from: LocalDate = LocalDate.now(), limit: Int = Int.MAX_VALUE): List<PickupDay> =
        _state.value.data?.pickups?.filter { d -> d.localDate?.let { !it.isBefore(from) } == true }?.take(limit) ?: emptyList()

    companion object {
        const val STALE_AFTER_MS = 6 * 60 * 60 * 1000L
    }
}
