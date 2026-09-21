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

import java.util.Locale

/** One pin to hand to a map app. [group] becomes a KML folder, so the list stays sorted by stream. */
data class MapPlace(
    val name: String,
    val description: String,
    val group: String,
    val latitude: Double,
    val longitude: Double,
    val colorArgb: Int,
)

/**
 * Builds a KML document out of [places]. Grouping into one folder per waste stream is what makes
 * this readable in a map app: the places pane shows "Glas (12)", "Papier (8)" and so on rather than
 * one flat pile of identical pins.
 */
object KmlExport {

    fun build(title: String, places: List<MapPlace>): String {
        val groups = places.groupBy { it.group }
        val styles = places.associateBy({ it.group }, { it.colorArgb })
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n")
        sb.append("  <name>").append(escape(title)).append("</name>\n")
        styles.forEach { (group, argb) ->
            sb.append("  <Style id=\"").append(styleId(group)).append("\">\n")
            sb.append("    <IconStyle>\n")
            sb.append("      <color>").append(kmlColor(argb)).append("</color>\n")
            sb.append("      <scale>1.1</scale>\n")
            sb.append("      <Icon><href>https://maps.google.com/mapfiles/kml/paddle/wht-blank.png</href></Icon>\n")
            sb.append("    </IconStyle>\n")
            sb.append("  </Style>\n")
        }
        groups.forEach { (group, list) ->
            sb.append("  <Folder>\n")
            sb.append("    <name>").append(escape("$group (${list.size})")).append("</name>\n")
            list.forEach { p ->
                sb.append("    <Placemark>\n")
                sb.append("      <name>").append(escape(p.name)).append("</name>\n")
                if (p.description.isNotBlank()) {
                    sb.append("      <description>").append(escape(p.description)).append("</description>\n")
                }
                sb.append("      <styleUrl>#").append(styleId(group)).append("</styleUrl>\n")
                sb.append("      <Point><coordinates>")
                    .append(coord(p.longitude)).append(',').append(coord(p.latitude)).append(",0")
                    .append("</coordinates></Point>\n")
                sb.append("    </Placemark>\n")
            }
            sb.append("  </Folder>\n")
        }
        sb.append("</Document>\n</kml>\n")
        return sb.toString()
    }

    /** KML wants `aabbggrr`, the reverse byte order of an Android ARGB int. */
    private fun kmlColor(argb: Int): String = String.format(
        Locale.ROOT, "%02x%02x%02x%02x",
        (argb ushr 24) and 0xFF, argb and 0xFF, (argb ushr 8) and 0xFF, (argb ushr 16) and 0xFF,
    )

    private fun coord(value: Double): String = String.format(Locale.ROOT, "%.6f", value)

    private fun styleId(group: String): String =
        "s_" + group.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "other" }

    private fun escape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}
