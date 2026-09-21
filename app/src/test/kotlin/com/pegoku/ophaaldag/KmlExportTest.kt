package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.util.KmlExport
import com.pegoku.ophaaldag.util.MapPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KmlExportTest {

    private val places = listOf(
        MapPlace("Klipperstraat 10", "Glas · Eindhoven · 240 m", "Glas", 51.4416, 5.4697, 0xFF00838F.toInt()),
        MapPlace("Markt 21 D", "Glas · Eindhoven · 1,2 km", "Glas", 51.4392, 5.4781, 0xFF00838F.toInt()),
        MapPlace("Fellenoord 1 & 2", "Papier · Eindhoven", "Papier", 51.4450, 5.4750, 0xFF1565C0.toInt()),
    )

    @Test
    fun groupsPlacesIntoOneFolderPerWasteStream() {
        val kml = KmlExport.build("Containers nearby", places)
        assertEquals(2, Regex("<Folder>").findAll(kml).count())
        assertTrue(kml.contains("<name>Glas (2)</name>"))
        assertTrue(kml.contains("<name>Papier (1)</name>"))
        assertEquals(3, Regex("<Placemark>").findAll(kml).count())
    }

    @Test
    fun writesLonLatOrderWithADotDecimalSeparator() {
        val kml = KmlExport.build("Containers nearby", places)
        assertTrue(kml.contains("<coordinates>5.469700,51.441600,0</coordinates>"))
    }

    @Test
    fun convertsArgbToKmlsReversedByteOrder() {
        val kml = KmlExport.build("Containers nearby", places)
        // 0xFF00838F ARGB -> aabbggrr
        assertTrue(kml.contains("<color>ff8f8300</color>"))
        assertTrue(kml.contains("<color>ffc06515</color>"))
    }

    @Test
    fun escapesXmlInNamesAndDescriptions() {
        val kml = KmlExport.build("Containers & co", places)
        assertTrue(kml.contains("<name>Containers &amp; co</name>"))
        assertTrue(kml.contains("Fellenoord 1 &amp; 2"))
    }

    @Test
    fun emptyInputStillProducesAValidDocument() {
        val kml = KmlExport.build("Containers nearby", emptyList())
        assertTrue(kml.startsWith("<?xml"))
        assertTrue(kml.trimEnd().endsWith("</kml>"))
        assertTrue(!kml.contains("<Folder>"))
    }
}
