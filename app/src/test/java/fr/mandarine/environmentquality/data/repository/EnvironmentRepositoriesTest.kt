package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.FakeHttpClient
import fr.mandarine.environmentquality.domain.model.Audience
import fr.mandarine.environmentquality.domain.model.ClayExposure
import fr.mandarine.environmentquality.domain.model.DroughtLevel
import fr.mandarine.environmentquality.domain.model.FloodAreaKind
import fr.mandarine.environmentquality.domain.model.FlowState
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.RiskPlanKind
import fr.mandarine.environmentquality.domain.model.SevesoStatus
import fr.mandarine.environmentquality.domain.model.WaterResource
import fr.mandarine.environmentquality.domain.model.byDay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        val temperatures = repository.recentTemperatures("03174000")
        assertEquals(LocalDateTime.of(2024, 8, 11, 9, 0), temperatures.first().measuredAt)
        val days = temperatures.byDay()
        assertEquals(listOf(LocalDate.of(2024, 8, 9), LocalDate.of(2024, 8, 10), LocalDate.of(2024, 8, 11)), days.map { it.date })
        assertEquals(23.112, days[1].minCelsius, 0.001)
        assertEquals(24.219, days[1].maxCelsius, 0.001)
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

    @Test
    fun `should keep each station's latest flow class when reading observations`() = runTest {
        val http = FakeHttpClient(listOf("ecoulement/observations" to "ecoulement_observations.json"))

        val flows = HubEauStationRepository(http).latestFlows(listOf("H3120421", "H3150001"))

        assertEquals(FlowState.FLOWING, flows.getValue("H3120421").state)
        assertEquals(LocalDate.of(2026, 9, 23), flows.getValue("H3150001").observedOn)
        assertTrue("code_station=H3120421%2CH3150001" in http.requests.single())
    }

    @Test
    fun `should make no request when there is no flow station to observe`() = runTest {
        val http = FakeHttpClient(emptyList())

        assertTrue(HubEauStationRepository(http).latestFlows(emptyList()).isEmpty())
        assertTrue(http.requests.isEmpty())
    }

    @Test
    fun `should read the last sampling day and the water bodies when listing the commune's groundwater stations`() = runTest {
        val http = FakeHttpClient(listOf("qualite_nappes/stations" to "nappes_stations_vernon.json"))

        val stations = HubEauStationRepository(http).groundwaterStations("27681", "27")

        val latest = stations.first { it.code == "BSS000LDUS" }
        assertEquals(LocalDate.of(2025, 11, 12), latest.lastSampledOn)
        assertEquals("Craie du Vexin normand et picard", latest.waterBody)
        assertEquals(15.0, latest.altitude!!, 1e-9)
        assertNull(stations.first { it.code == "BSS000LECJ" }.lastSampledOn)
    }

    @Test
    fun `should read nitrates, pesticides and quantified substances when reading a groundwater sampling`() = runTest {
        val http = FakeHttpClient(listOf("qualite_nappes/analyses" to "nappes_analyses.json"))

        val sampling = HubEauStationRepository(http).groundwaterSampling("BSS000LDUS", LocalDate.of(2025, 11, 12))!!

        assertEquals(LocalDate.of(2025, 11, 12), sampling.sampledOn)
        assertEquals(44, sampling.results.size)
        assertEquals(34, sampling.quantifiedCount)
        assertEquals(16.0, sampling.nitrates!!.value!!, 1e-9)
        assertEquals("mg(NO3)/L", sampling.nitrates!!.unit)
        assertEquals(0.13, sampling.pesticides!!.value!!, 1e-9)
        assertTrue("bss_id=BSS000LDUS" in http.requests.single())
        assertTrue("date_debut_prelevement=2025-11-12" in http.requests.single())
    }

    @Test
    fun `should report no sampling when the station has no analysis`() = runTest {
        val http = FakeHttpClient(listOf("qualite_nappes/analyses" to "hubeau_empty.json"))

        assertNull(HubEauStationRepository(http).groundwaterSampling("BSS000JKHP", LocalDate.of(2020, 1, 1)))
    }

    private val detailRoutes = listOf(
        "gaspar/pprn" to "georisques_pprn.json",
        "gaspar/pprt" to "georisques_pprt.json",
        "gaspar/pprm" to "georisques_pprm.json",
        "gaspar/tri" to "georisques_tri.json",
        "gaspar/azi" to "georisques_azi.json",
        "ssp/conclusions_sis" to "georisques_sis.json",
        "ssp/instructions" to "georisques_ssp_instructions.json",
        "ssp/casias" to "georisques_casias.json",
    )

    private val vernon = GeoPoint(49.0921, 1.4827)

    @Test
    fun `should read plans, flood atlases and polluted soils when loading the risk details`() = runTest {
        val http = FakeHttpClient(listOf("/rga" to "empty_body.txt") + detailRoutes)

        val details = GeorisquesRiskRepository(http).riskDetails("27681", vernon)

        assertEquals(ClayExposure.NONE, details.clay)
        assertEquals(listOf(RiskPlanKind.NATURAL, RiskPlanKind.TECHNOLOGICAL), details.plans.map { it.kind })
        assertEquals("PPRN-I - Seine dans l'Eure", details.plans.first().name)
        assertEquals(LocalDate.of(2026, 2, 3), details.plans.first().updated)
        assertEquals(listOf("Atlas des PHEC", "AZI SEINE"), details.floodAreas.map { it.name })
        assertTrue(details.floodAreas.all { it.kind == FloodAreaKind.FLOOD_ATLAS })
        val soils = details.soils!!
        assertEquals(6, soils.sectors.size)
        assertEquals("68 Avenue de Rouen", soils.sectors.first { it.name == "ENGIE" }.address)
        assertEquals(null, soils.sectors.first { it.name == "Ralston Energy Systems" }.address)
        assertEquals(8, soils.actionSites)
        assertEquals(1, soils.openActions)
        assertEquals(109, soils.formerActivities)
        assertTrue(http.requests.any { "rga?latlon=1.4827%2C49.0921" in it })
        assertTrue(http.requests.any { "ssp/casias" in it && "page_size=1" in it })
    }

    @Test
    fun `should read the exposure class when the centre lies on mapped clay`() = runTest {
        val http = FakeHttpClient(listOf("/rga" to "georisques_rga.json") + detailRoutes)

        assertEquals(ClayExposure.MEDIUM, GeorisquesRiskRepository(http).riskDetails("69123", GeoPoint(45.764, 4.8357)).clay)
    }

    @Test
    fun `should keep the other details and leave clay unknown when a source fails or the centre is missing`() = runTest {
        val http = FakeHttpClient(detailRoutes.filterNot { it.first == "gaspar/azi" || it.first == "ssp/casias" })

        val details = GeorisquesRiskRepository(http).riskDetails("27681", centre = null)

        assertNull(details.clay)
        assertTrue(details.floodAreas.isEmpty())
        assertEquals(2, details.plans.size)
        assertNull(details.soils!!.formerActivities)
        assertEquals(8, details.soils!!.actionSites)
        assertTrue(http.requests.none { "/rga" in it })
    }

    @Test
    fun `should report no polluted soils information when none of the inventories answers`() = runTest {
        val http = FakeHttpClient(detailRoutes.filterNot { it.first.startsWith("ssp/") })

        assertNull(GeorisquesRiskRepository(http).riskDetails("27681", vernon).soils)
    }
}
