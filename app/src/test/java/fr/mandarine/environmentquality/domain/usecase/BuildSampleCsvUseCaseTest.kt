package fr.mandarine.environmentquality.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildSampleCsvUseCaseTest {

    private val build = BuildSampleCsvUseCase()

    @Test
    fun `should write a header and one row per parameter when exporting a sample`() {
        val lines = build(vernon, sample()).trimEnd().lines()

        assertEquals(3, lines.size)
        assertEquals(
            "27681;Vernon;2026-07-29;02700176757;VERNON NORMANDIE;1340;Nitrates (en NO3);50,0;mg/L;<=50 mg/L;",
            lines[1].trimEnd('\r'),
        )
    }

    @Test
    fun `should quote a field when it contains a separator`() {
        val csv = build(vernon.copy(name = "Saint-Denis; Réunion"), sample())

        assertEquals(true, csv.contains("\"Saint-Denis; Réunion\""))
    }

    @Test
    fun `should name the file after the commune and the sampling date`() {
        assertEquals("eau-27681-20260729.csv", build.fileName(vernon, sample()))
    }
}
