package fr.mandarine.environmentquality.presentation

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.FlowObservation
import fr.mandarine.environmentquality.domain.model.FlowState
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.GroundwaterLevel
import fr.mandarine.environmentquality.domain.model.GroundwaterSampling
import fr.mandarine.environmentquality.domain.model.GroundwaterStation
import fr.mandarine.environmentquality.domain.model.HydroReading
import fr.mandarine.environmentquality.domain.model.HydroStation
import fr.mandarine.environmentquality.domain.model.PiezometerStation
import fr.mandarine.environmentquality.domain.model.RiverQuality
import fr.mandarine.environmentquality.domain.model.RiverResult
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.StationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class NearbyViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val vernon = Commune("27681", "Vernon", departmentCode = "27", centre = GeoPoint(49.09, 1.48))
    private val communes = mockk<CommuneRepository> { coEvery { byCode("27681") } returns vernon }

    private fun hydro(code: String, latitude: Double, inService: Boolean = true) =
        HydroStation(code, code, "La Seine", GeoPoint(latitude, 1.48), inService, null)

    private fun flow(code: String, latitude: Double) = FlowStation(code, code, null, null, true, GeoPoint(latitude, 1.48))

    private val reading = HydroReading(2.1, null, 450.0, null, 440.0, 5.0)

    private fun well(code: String, latitude: Double, lastSampledOn: LocalDate?) =
        GroundwaterStation(code, "Vernon", "Forage", null, null, GeoPoint(latitude, 1.48), lastSampledOn)

    private fun stationsAround(groundwater: List<GroundwaterStation>) = mockk<StationRepository> {
        coEvery { hydroStations("27") } returns emptyList()
        coEvery { temperatureStations("27") } returns emptyList()
        coEvery { flowStations("27") } returns listOf(flow("f1", 49.1), flow("f2", 49.2))
        coEvery { latestFlows(listOf("f1", "f2")) } returns mapOf("f2" to FlowObservation(FlowState.DRY, LocalDate.of(2026, 9, 23)))
        coEvery { groundwaterStations("27681", "27") } returns groundwater
        coEvery { groundwaterSampling(any(), any()) } returns GroundwaterSampling(LocalDate.of(2025, 11, 12), emptyList())
    }

    private fun TestScope.nearbyOf(stations: StationRepository): Nearby {
        val viewModel = NearbyViewModel("27681", communes, stations)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel.state.value.data.valueOrNull!!
    }

    @Test
    fun `should pick the nearest reporting river station when the closest one is silent`() = runTest {
        val stations = mockk<StationRepository> {
            coEvery { hydroStations("27") } returns listOf(hydro("far", 49.9), hydro("closed", 49.09, inService = false), hydro("near", 49.1), hydro("mid", 49.3))
            coEvery { hydroReading("near") } returns null
            coEvery { hydroReading("mid") } returns reading
            coEvery { temperatureStations("27") } returns emptyList()
            coEvery { flowStations("27") } returns listOf(flow("f4", 49.8), flow("f1", 49.1), flow("f3", 49.5), flow("f2", 49.2))
            coEvery { groundwaterStations("27681", "27") } throws java.io.IOException("down")
        }
        val viewModel = NearbyViewModel("27681", communes, stations)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        val nearby = viewModel.state.value.data.valueOrNull!!
        assertEquals("mid", nearby.river?.station?.code)
        assertEquals(listOf("f1", "f2", "f3"), nearby.flowStations.map { it.code })
        assertEquals(4, nearby.flowStationCount)
        assertNull(nearby.temperature)
        assertNull(nearby.groundwater)
    }

    @Test
    fun `should pick the nearest recently sampled groundwater station and read its sampling when old wells are closer`() = runTest {
        val stations = stationsAround(
            listOf(
                well("closed-1998", 49.09, LocalDate.of(1998, 2, 15)),
                well("never", 49.09, null),
                well("older", 49.12, LocalDate.of(2024, 5, 23)),
                well("same-spot-older", 49.11, LocalDate.of(2025, 1, 22)),
                well("latest", 49.11, LocalDate.of(2025, 11, 12)),
            ),
        )

        val nearby = nearbyOf(stations)

        assertEquals("latest", nearby.groundwater?.code)
        assertEquals(LocalDate.of(2025, 11, 12), nearby.groundwaterSampling?.sampledOn)
        coVerify { stations.groundwaterSampling("latest", LocalDate.of(2025, 11, 12)) }
    }

    @Test
    fun `should keep the nearest station without reading a sampling when none of them reports a sampling day`() = runTest {
        val stations = stationsAround(listOf(well("far", 49.5, null), well("near", 49.1, null)))

        val nearby = nearbyOf(stations)

        assertEquals("near", nearby.groundwater?.code)
        assertNull(nearby.groundwaterSampling)
        coVerify(exactly = 0) { stations.groundwaterSampling(any(), any()) }
    }

    @Test
    fun `should attach the latest observation to the shown flow stations when they have one`() = runTest {
        val nearby = nearbyOf(stationsAround(emptyList()))

        assertEquals(FlowState.DRY, nearby.flows["f2"]?.state)
        assertNull(nearby.flows["f1"])
    }

    private fun piezometer(code: String, latitude: Double, lastMeasuredOn: LocalDate?) =
        PiezometerStation(code, code, null, GeoPoint(latitude, 1.48), lastMeasuredOn)

    private fun level(day: LocalDate, metres: Double) = GroundwaterLevel(day, metres, 10.0)

    @Test
    fun `should compare the nearest reporting piezometer's level with four weeks before when stale wells are closer`() = runTest {
        val today = LocalDate.of(2026, 10, 16)
        val stations = stationsAround(emptyList())
        coEvery { stations.piezometers("27") } returns listOf(
            piezometer("stopped-2019", 49.09, LocalDate.of(2019, 10, 22)),
            piezometer("silent", 49.10, today),
            piezometer("reporting", 49.12, today.minusDays(1)),
            piezometer("far", 49.6, today),
        )
        coEvery { stations.recentLevels("silent") } returns emptyList()
        coEvery { stations.recentLevels("reporting") } returns (0L..40L).map { level(today.minusDays(1 + it), 100.0 - it * 0.01) }
        coEvery { stations.riverQuality(any()) } returns null

        val level = nearbyOf(stations).aquiferLevel!!

        assertEquals("reporting", level.station.code)
        assertEquals(today.minusDays(1), level.latest.measuredOn)
        assertEquals(today.minusDays(29), level.monthAgo!!.measuredOn)
        assertEquals(0.28, level.latest.levelMetres - level.monthAgo!!.levelMetres, 1e-9)
    }

    @Test
    fun `should ask for river quality at the commune's centre and give its distance`() = runTest {
        val stations = stationsAround(emptyList())
        val quality = RiverQuality("03172150", "RU DE BLARU A VERNON 1", GeoPoint(49.09, 1.48), listOf(RiverResult(1340, "Nitrates", 49.2, "mg(NO3)/L", LocalDate.of(2026, 6, 1))))
        coEvery { stations.piezometers("27") } returns emptyList()
        coEvery { stations.riverQuality(vernon.centre!!) } returns quality

        val nearby = nearbyOf(stations)

        assertEquals(quality, nearby.riverQuality?.quality)
        assertEquals(0.0, nearby.riverQuality!!.distanceKm!!, 0.5)
        assertNull(nearby.aquiferLevel)
    }
}
