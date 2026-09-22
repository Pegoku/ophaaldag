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
package com.pegoku.ophaaldag.map

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

/**
 * Background map tiles for the container map.
 *
 * Deliberately *not* OpenStreetMap's own tile server: its tile usage policy rules out apps
 * distributed at any scale. These come from PDOK, the Dutch government's open geo service run by
 * the Kadaster: no API key, no registration, no quota, and open data under CC BY 4.0. Coverage is
 * the Netherlands only, which is exactly Cure's service area. Attribution is shown on the map.
 */
object PdokTiles {

    const val ATTRIBUTION = "© Kadaster"

    /** The grey variant: a quiet basemap that leaves the coloured container pins legible. */
    val source: OnlineTileSourceBase = XYTileSource(
        "PDOK-BRT-grijs",
        6,
        19,
        256,
        ".png",
        arrayOf("https://service.pdok.nl/brt/achtergrondkaart/wmts/v2_0/grijs/EPSG:3857/"),
        ATTRIBUTION,
    )

    /**
     * osmdroid defaults its cache to external storage. Point it at the app's own cache dir instead
     * so the map needs no storage permission and gets cleaned up with the rest of the cache.
     */
    fun configure(context: Context) {
        val config = Configuration.getInstance()
        if (config.userAgentValue == context.packageName) return
        config.userAgentValue = context.packageName
        config.osmdroidBasePath = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        config.osmdroidTileCache = File(config.osmdroidBasePath, "tiles").apply { mkdirs() }
    }
}
