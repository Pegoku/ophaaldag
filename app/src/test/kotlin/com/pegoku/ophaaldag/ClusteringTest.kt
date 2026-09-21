package com.pegoku.ophaaldag

import com.pegoku.ophaaldag.data.ContainerLocation
import com.pegoku.ophaaldag.map.clusterContainers
import com.pegoku.ophaaldag.util.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusteringTest {

    private fun container(type: String, address: String, lat: Double, lon: Double) =
        ContainerLocation(wasteType = type, address = address, city = "Eindhoven", latitude = "$lat", longitude = "$lon")

    // Three at one address plus a glass bank a few metres away, and one a full street away.
    private val sameSpot = listOf(
        container("restafval", "Emmasingel 33", 51.44100, 5.47300),
        container("restafval", "Emmasingel 33", 51.44101, 5.47301),
        container("glas", "Emmasingel 33", 51.44102, 5.47302),
    )
    private val faraway = container("papier", "Willemstraat 3", 51.44500, 5.47800)

    @Test
    fun collapsesNeighboursAndLeavesDistantOnesAlone() {
        val clusters = clusterContainers(sameSpot + faraway, radiusMeters = 50.0)
        assertEquals(2, clusters.size)
        assertEquals(3, clusters[0].size)
        assertEquals(1, clusters[1].size)
    }

    @Test
    fun clusterListsEachStreamOnceInEncounterOrder() {
        val clusters = clusterContainers(sameSpot, radiusMeters = 50.0)
        assertEquals(listOf("restafval", "glas"), clusters.single().types)
    }

    @Test
    fun aZeroRadiusLeavesEveryContainerOnItsOwn() {
        val clusters = clusterContainers(sameSpot + faraway, radiusMeters = 0.0)
        assertEquals(4, clusters.size)
        assertTrue(clusters.all { it.size == 1 })
    }

    @Test
    fun clusterSitsAtTheCentroidOfItsMembers() {
        val cluster = clusterContainers(sameSpot, radiusMeters = 50.0).single()
        assertEquals(51.44101, cluster.latitude, 1e-6)
        assertEquals(5.47301, cluster.longitude, 1e-6)
    }

    @Test
    fun sharedAddressIsOnlySetWhenEveryMemberAgrees() {
        assertEquals("Emmasingel 33", clusterContainers(sameSpot, 50.0).single().sharedAddress)
        assertNull(clusterContainers(sameSpot + faraway, radiusMeters = 1000.0).single().sharedAddress)
    }

    @Test
    fun clusterRadiusShrinksAsYouZoomIn() {
        val atZoom15 = Geo.metersPerDp(51.44, 15)
        val atZoom18 = Geo.metersPerDp(51.44, 18)
        assertEquals(8.0, atZoom15 / atZoom18, 1e-9)
        // Sanity-check the absolute scale: ~3 m per dp over Eindhoven at zoom 15.
        assertEquals(2.98, atZoom15, 0.05)
    }
}
