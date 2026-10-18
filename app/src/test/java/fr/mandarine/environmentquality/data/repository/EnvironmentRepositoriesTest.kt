package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.FakeHttpClient
import fr.mandarine.environmentquality.domain.model.Audience
import fr.mandarine.environmentquality.domain.model.DroughtLevel
import fr.mandarine.environmentquality.domain.model.SevesoStatus
import fr.mandarine.environmentquality.domain.model.WaterResource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class EnvironmentRepositoriesTest {

    @Test
    fun `should read level, decree dates and audiences when parsing VigiEau zones`() = runTest {
        val status = VigieauDroughtRepository(FakeHttpClient(listOf("zones" to "vigieau_zones.json"))).status("27681")

        assertEquals(DroughtLevel.VIGILANCE, status.level)
        val zone = status.zones.first()
        assertEquals("EURE MOYENNE", zone.name)
        assertEquals(WaterResource.GROUNDWATER, zone.resource)
        assertEquals(LocalDate.of(2026, 9, 14), zone.validFrom)
        assertEquals(LocalDate.of(2026, 11, 30), zone.validUntil)
        assertEquals(
            listOf("Arrosage des jardins potagers", "Lavage de véhicules chez les particuliers", "Activités nautiques"),
            status.usesFor(Audience.INDIVIDUALS).map { it.name },
        )
    }

    @Test
    fun `should report no restriction when VigiEau returns an empty array`() = runTest {
        val status = VigieauDroughtRepository(FakeHttpClient(listOf("zones" to "empty_array.json"))).status("75056")

        assertEquals(DroughtLevel.NONE, status.level)
    }

    @Test
    fun `should nest risk types under their family and merge the five Georisques calls`() = runTest {
        val http = FakeHttpClient(
            listOf(
                "gaspar/risques" to "georisques_risques.json",
                "gaspar/catnat" to "georisques_catnat.json",
                "radon" to "georisques_radon.json",
                "zonage_sismique" to "georisques_sismique.json",
                "installations_classees" to "georisques_installations.json",
            ),
        )

        val risks = GeorisquesRiskRepository(http).risks("27681")

        assertEquals(listOf("Inondation", "Mouvement de terrain", "Risque industriel", "Transport de marchandises dangereuses"), risks.groups.map { it.name })
        assertEquals(3, risks.groups.first { it.code == "12" }.details.size)
        assertEquals(13, risks.riskCount)
        assertEquals(LocalDate.of(2001, 3, 25), risks.disasters.first().start)
        assertEquals(1, risks.radonClass)
        assertEquals(1, risks.seismicZone)
        assertEquals("ARIANEGROUP SAS", risks.sites.first().name)
        assertEquals(SevesoStatus.HIGH, risks.sites.first().seveso)
        assertEquals(1, risks.sevesoCount)
    }

    @Test
    fun `should keep the latest inspection and its report when a site has several visits`() = runTest {
        val http = FakeHttpClient(
            listOf(
                "gaspar/risques" to "georisques_risques.json",
                "gaspar/catnat" to "georisques_catnat.json",
                "radon" to "georisques_radon.json",
                "zonage_sismique" to "georisques_sismique.json",
                "installations_classees" to "georisques_installations.json",
            ),
        )

        val sites = GeorisquesRiskRepository(http).risks("27681").sites
        val ariane = sites.first { it.name == "ARIANEGROUP SAS" }
        val quarry = sites.first { it.name.startsWith("Carrières") }

        assertEquals(LocalDate.of(2025, 3, 25), ariane.lastInspection)
        assertEquals("https://www.georisques.gouv.fr/webappReport/ws/installations/inspection/I1fQZsy0SVPka3suw0gaJhjeNvmTs0O7", ariane.inspectionReportUrl)
        assertEquals(LocalDate.of(2017, 4, 3), quarry.lastInspection)
        assertEquals(null, quarry.inspectionReportUrl)
    }

    @Test
    fun `should convert millimetres and litres per second when reading live river data`() = runTest {
        val repository = HubEauStationRepository(FakeHttpClient(listOf("observations_tr" to "hydro_observations.json")))

        val reading = repository.hydroReading("H320000104")!!

        assertEquals(3.229, reading.heightMetres!!, 1e-9)
        assertEquals(168.0, reading.flowCubicMetres!!, 1e-9)
        assertEquals(176.0, reading.previousFlowCubicMetres!!, 1e-9)
        assertEquals(-5.0, reading.heightChangeMillimetres!!, 1e-9)
        assertEquals(Instant.parse("2026-10-18T11:00:00Z"), reading.heightAt)
    }

    @Test
    fun `should list stations and keep the measurement date when reading temperature and flow`() = runTest {
        val repository = HubEauStationRepository(
            FakeHttpClient(
                listOf(
                    "referentiel/stations" to "hydro_stations.json",
                    "temperature/chronique" to "temperature_chronique.json",
                    "ecoulement/stations" to "ecoulement_stations.json",
                ),
            ),
        )

        assertEquals("La Seine", repository.hydroStations("27")[1].river)
        assertEquals(LocalDateTime.of(2024, 8, 11, 9, 0), repository.latestTemperature("03174000")!!.measuredAt)
        val flows = repository.flowStations("27")
        assertTrue(flows[0].active)
        assertFalse(flows[1].active)
    }

    @Test
    fun `should drop groundwater points without a location when falling back to the department`() = runTest {
        val http = FakeHttpClient(listOf("code_commune" to "hubeau_empty.json", "code_departement" to "nappes_stations.json"))

        val stations = HubEauStationRepository(http).groundwaterStations("27681", "27")

        assertEquals("BSS000JKHP", stations.single().code)
        assertEquals("Source", stations.single().nature)
    }
}
