package fr.mandarine.environmentquality.domain.model

import fr.mandarine.environmentquality.domain.model.Conformity.CONFORM
import fr.mandarine.environmentquality.domain.model.Conformity.NON_CONFORM
import fr.mandarine.environmentquality.domain.model.Conformity.NOT_ANALYSED
import org.junit.Assert.assertEquals
import org.junit.Test

class ConformityFlagsTest {

    @Test
    fun `should map Hub'Eau codes when parsing a flag`() {
        assertEquals(CONFORM, Conformity.fromCode("C"))
        assertEquals(NON_CONFORM, Conformity.fromCode("N"))
        assertEquals(NOT_ANALYSED, Conformity.fromCode("S"))
        assertEquals(NOT_ANALYSED, Conformity.fromCode(null))
    }

    @Test
    fun `should be conform when the analysed families pass and the others were not analysed`() {
        val flags = ConformityFlags(NOT_ANALYSED, CONFORM, NOT_ANALYSED, CONFORM)

        assertEquals(WaterStatus.CONFORM, flags.status)
    }

    @Test
    fun `should have reserves when only a reference is breached`() {
        val flags = ConformityFlags(CONFORM, CONFORM, CONFORM, NON_CONFORM)

        assertEquals(WaterStatus.RESERVES, flags.status)
    }

    @Test
    fun `should be non conform when a limit is breached whatever the references`() {
        val flags = ConformityFlags(NON_CONFORM, CONFORM, CONFORM, NON_CONFORM)

        assertEquals(WaterStatus.NON_CONFORM, flags.status)
    }

    @Test
    fun `should be unknown when nothing was analysed`() {
        val flags = ConformityFlags(NOT_ANALYSED, NOT_ANALYSED, NOT_ANALYSED, NOT_ANALYSED)

        assertEquals(WaterStatus.UNKNOWN, flags.status)
    }
}
