package fr.mandarine.environmentquality.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPointTest {

    @Test
    fun `should give the great-circle distance in km when measuring Paris to Lyon`() {
        val paris = GeoPoint(48.8566, 2.3522)
        val lyon = GeoPoint(45.764, 4.8357)
        assertEquals(392.0, paris.distanceKm(lyon), 3.0)
    }

    @Test
    fun `should give zero when both points are the same`() {
        val vernon = GeoPoint(49.09, 1.48)
        assertEquals(0.0, vernon.distanceKm(vernon), 1e-9)
    }
}
