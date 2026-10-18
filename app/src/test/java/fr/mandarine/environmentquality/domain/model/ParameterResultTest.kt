package fr.mandarine.environmentquality.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParameterResultTest {

    private fun result(
        value: Double?,
        display: String = value.toString(),
        limit: String? = null,
        reference: String? = null,
        code: String = "1340",
        name: String = "Nitrates (en NO3)",
    ) = ParameterResult(code, name, value, display, "mg/L", limit, reference)

    @Test
    fun `should parse an upper bound when the text has a single inequality`() {
        assertEquals(QualityRange(null, 50.0), QualityRange.parse("<=50 mg/L"))
    }

    @Test
    fun `should parse both bounds with decimal commas when the text is a range`() {
        assertEquals(QualityRange(6.5, 9.0), QualityRange.parse(">=6,5 et <=9 unité pH"))
        assertEquals(QualityRange(200.0, 1100.0), QualityRange.parse(">=200 et <=1100 µS/cm"))
    }

    @Test
    fun `should return null when the text has no bound`() {
        assertNull(QualityRange.parse("Absence"))
        assertNull(QualityRange.parse(null))
    }

    @Test
    fun `should be near the limit but not over it when the value equals the limit`() {
        val nitrates = result(50.0, "50,0", limit = "<=50 mg/L")

        assertFalse(nitrates.exceedsLimit)
        assertTrue(nitrates.nearLimit)
        assertEquals(1f, nitrates.fillRatio!!, 0.001f)
    }

    @Test
    fun `should exceed the limit when the value is above it`() {
        assertTrue(result(52.0, "52,0", limit = "<=50 mg/L").exceedsLimit)
    }

    @Test
    fun `should not exceed anything when the result is below detection`() {
        val turbidity = result(0.3, "<0,30", reference = "<=0,1 NFU", code = "1295", name = "Turbidité")

        assertFalse(turbidity.exceedsReference)
    }

    @Test
    fun `should exceed the reference when the value is outside the range`() {
        assertTrue(result(1120.0, reference = ">=200 et <=1100 µS/cm").exceedsReference)
        assertTrue(result(150.0, reference = ">=200 et <=1100 µS/cm").exceedsReference)
    }

    @Test
    fun `should group parameters by code and label`() {
        assertEquals(ParameterGroup.NITRATES, ParameterResult.groupOf("1340", "Nitrates (en NO3)"))
        assertEquals(ParameterGroup.PHYSICAL, ParameterResult.groupOf("1302", "pH"))
        assertEquals(ParameterGroup.CHLORINE, ParameterResult.groupOf("1398", "Chlore libre"))
        assertEquals(ParameterGroup.SENSORY, ParameterResult.groupOf("5902", "Saveur (qualitatif)"))
        assertEquals(ParameterGroup.BACTERIOLOGY, ParameterResult.groupOf("1449", "Escherichia coli /100ml - MF"))
    }

    @Test
    fun `should hide the unit when Hub'Eau says it does not apply`() {
        val taste = ParameterResult("5902", "Saveur", 0.0, "Aucun changement anormal", "SANS OBJET", null, null)

        assertNull(taste.unitLabel)
    }
}
