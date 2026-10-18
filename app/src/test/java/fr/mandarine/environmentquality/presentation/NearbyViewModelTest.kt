package fr.mandarine.environmentquality.presentation

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.HydroReading
import fr.mandarine.environmentquality.domain.model.HydroStation
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.StationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class NearbyViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val vernon = Commune("27681", "Vernon", departmentCode = "27", centre = GeoPoint(49.09, 1.48))
    private val communes = mockk<CommuneRepository> { coEvery { byCode("27681") } returns vernon }

    private fun hydro(code: String, latitude: Double, inService: Boolean = true) =
        HydroStation(code, code, "La Seine", GeoPoint(latitude, 1.48), inService, null)

    private fun flow(code: String, latitude: Double) = FlowStation(code, code, null, null, true, GeoPoint(latitude, 1.48))

    private val reading = HydroReading(2.1, null, 450.0, null, 440.0, 5.0)

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
}
