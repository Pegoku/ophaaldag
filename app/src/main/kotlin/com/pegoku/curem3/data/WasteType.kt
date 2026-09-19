package com.pegoku.curem3.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Visual identity for a waste stream id as used by the API (`type` / `iconName`). */
data class WasteStyle(val icon: ImageVector, val color: Color)

object WasteTypes {
    private val styles: Map<String, WasteStyle> = mapOf(
        "restafval" to WasteStyle(Icons.Outlined.Delete, Color(0xFF5F6368)),
        "gft" to WasteStyle(Icons.Outlined.Eco, Color(0xFF2E7D32)),
        "papier" to WasteStyle(Icons.Outlined.Newspaper, Color(0xFF1565C0)),
        "pmd" to WasteStyle(Icons.Outlined.Recycling, Color(0xFFEF6C00)),
        "pbd" to WasteStyle(Icons.Outlined.Recycling, Color(0xFFEF6C00)),
        "plastic" to WasteStyle(Icons.Outlined.Recycling, Color(0xFFEF6C00)),
        "glas" to WasteStyle(Icons.Outlined.WineBar, Color(0xFF00838F)),
        "textiel" to WasteStyle(Icons.Outlined.Checkroom, Color(0xFF6A1B9A)),
        "grofvuil" to WasteStyle(Icons.Outlined.Weekend, Color(0xFF6D4C41)),
        "kca" to WasteStyle(Icons.Outlined.Science, Color(0xFFC62828)),
        "elec" to WasteStyle(Icons.Outlined.ElectricalServices, Color(0xFFF9A825)),
        "milieustraat" to WasteStyle(Icons.Outlined.Storefront, Color(0xFF00695C)),
        "asbest" to WasteStyle(Icons.Outlined.Construction, Color(0xFF455A64)),
        "sloopafval" to WasteStyle(Icons.Outlined.Construction, Color(0xFF455A64)),
        "kerstbomen" to WasteStyle(Icons.Outlined.Park, Color(0xFF1B5E20)),
        "takken" to WasteStyle(Icons.Outlined.Forest, Color(0xFF33691E)),
        "luiers" to WasteStyle(Icons.Outlined.ChildCare, Color(0xFFAD1457)),
    )

    private val fallback = WasteStyle(Icons.Outlined.DeleteOutline, Color(0xFF757575))

    fun style(type: String): WasteStyle {
        val key = type.lowercase()
        styles[key]?.let { return it }
        return styles.entries.firstOrNull { key.contains(it.key) }?.value ?: fallback
    }
}
