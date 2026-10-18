package fr.mandarine.environmentquality.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DroughtStatusTest {

    private fun zone(level: DroughtLevel, vararg uses: WaterUse) =
        DroughtZone("Eure moyenne", WaterResource.SURFACE, level, null, null, null, uses.toList())

    @Test
    fun `should parse VigiEau levels when reading niveauGravite`() {
        assertEquals(DroughtLevel.VIGILANCE, DroughtLevel.fromCode("vigilance"))
        assertEquals(DroughtLevel.REINFORCED_ALERT, DroughtLevel.fromCode("alerte_renforcee"))
        assertEquals(DroughtLevel.CRISIS, DroughtLevel.fromCode("crise"))
        assertEquals(DroughtLevel.NONE, DroughtLevel.fromCode(null))
    }

    @Test
    fun `should take the worst level when zones differ`() {
        val status = DroughtStatus(listOf(zone(DroughtLevel.VIGILANCE), zone(DroughtLevel.ALERT)))

        assertEquals(DroughtLevel.ALERT, status.level)
    }

    @Test
    fun `should have no level when there is no zone`() {
        assertEquals(DroughtLevel.NONE, DroughtStatus(emptyList()).level)
    }

    @Test
    fun `should keep the strictest rule once when a use appears in several zones`() {
        val mild = WaterUse("Arrosage des jardins potagers", "Arroser", "Sensibiliser", setOf(Audience.INDIVIDUALS))
        val strict = mild.copy(rule = "Interdit de 9 h à 20 h")
        val status = DroughtStatus(listOf(zone(DroughtLevel.VIGILANCE, mild), zone(DroughtLevel.ALERT, strict)))

        assertEquals(listOf(strict), status.usesFor(Audience.INDIVIDUALS))
        assertEquals(emptyList<WaterUse>(), status.usesFor(Audience.FARMS))
    }
}
