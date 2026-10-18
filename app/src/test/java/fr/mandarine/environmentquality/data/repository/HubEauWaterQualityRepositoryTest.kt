package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.FakeHttpClient
import fr.mandarine.environmentquality.domain.model.WaterStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class HubEauWaterQualityRepositoryTest {

    private val http = FakeHttpClient(
        listOf(
            "code_parametre=" to "resultats_dis_history.json",
            "date_min_prelevement=2026-07-29" to "resultats_dis_day.json",
            "size=1&sort=desc" to "resultats_dis_latest.json",
        ),
    )
    private val repository = HubEauWaterQualityRepository(http)

    @Test
    fun `should read the latest sample status when asking for it`() = runTest {
        val latest = repository.latestStatus("27681")!!

        assertEquals("02700176757", latest.sampleCode)
        assertEquals(Instant.parse("2026-07-29T11:30:00Z"), latest.sampledAt)
        assertEquals(WaterStatus.CONFORM, latest.flags.status)
    }

    @Test
    fun `should keep only the latest sample's rows when the day has several samples`() = runTest {
        val sample = repository.latestSample("27681")!!

        assertEquals(listOf("5902", "1303", "1340"), sample.results.map { it.code })
        assertEquals("VERNON NORMANDIE", sample.networks.single().name)
        assertEquals("STGS", sample.distributor)
        assertTrue(http.requests.last().contains("date_max_prelevement=2026-07-30"))
    }

    @Test
    fun `should keep the published text and drop the fake zero when a result is qualitative`() = runTest {
        val taste = repository.latestSample("27681")!!.results.first { it.code == "5902" }

        assertEquals("Aucun changement anormal", taste.displayValue)
        assertNull(taste.numericValue)
    }

    @Test
    fun `should summarise twelve months with the worst status of each month`() = runTest {
        val history = repository.history("27681", LocalDate.of(2025, 8, 1))

        assertEquals(12, history.months.size)
        assertEquals(YearMonth.of(2025, 8), history.months.first().month)
        assertEquals(WaterStatus.UNKNOWN, history.months.first().status)
        assertEquals(WaterStatus.RESERVES, history.months.first { it.month == YearMonth.of(2026, 2) }.status)
        assertEquals(WaterStatus.CONFORM, history.months.last().status)
        assertEquals(2, history.conformMonths)
    }

    @Test
    fun `should plot the highest value of each month when building a series`() = runTest {
        val nitrates = repository.history("27681", LocalDate.of(2025, 8, 1)).series.first { it.code == "1340" }

        assertEquals(listOf(45.0, 46.0, 50.0), nitrates.points.map { it.value })
        assertEquals(50.0, nitrates.limit?.max)
    }

    @Test
    fun `should return null when the commune has no sample`() = runTest {
        val empty = HubEauWaterQualityRepository(FakeHttpClient(listOf("resultats_dis" to "hubeau_empty.json")))

        assertNull(empty.latestSample("00000"))
    }
}
