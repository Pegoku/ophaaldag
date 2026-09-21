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
package com.pegoku.ophaaldag.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.pegoku.ophaaldag.R
import java.io.File

/** Hands a whole set of container pins to whatever map app the user picks. */
object MapShare {

    private const val KML_MIME = "application/vnd.google-earth.kml+xml"

    /**
     * Writes [places] to a KML file in the cache and opens a chooser on it.
     *
     * A `geo:` intent only ever carries one pin, so a multi-point list has to travel as a file.
     * Map apps that import KML (Organic Maps, OsmAnd, Google Earth, Locus) open it directly; the
     * rest see it in the share sheet. Returns false when nothing on the device can take it.
     */
    suspend fun sharePlaces(context: Context, title: String, places: List<MapPlace>): Boolean {
        if (places.isEmpty()) return false
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, "maps").apply { mkdirs() }.resolve("containers.kml")
                .also { it.writeText(KmlExport.build(title, places)) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, KML_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val send = Intent(Intent.ACTION_SEND)
            .setType(KML_MIME)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val intent = if (view.resolveActivity(context.packageManager) != null) view else send
        val chooser = Intent.createChooser(intent, context.getString(R.string.containers_export_kml))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(chooser) }.isSuccess
    }
}
