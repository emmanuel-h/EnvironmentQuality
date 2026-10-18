package fr.mandarine.environmentquality.presentation

import fr.mandarine.environmentquality.domain.model.ClayExposure
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.RiskDetails
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.RiskRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RisksViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val centre = GeoPoint(49.0921, 1.4827)
    private val communes = mockk<CommuneRepository> { coEvery { byCode("27681") } returns Commune("27681", "Vernon", centre = centre) }
    private val summary = CommuneRisks(emptyList(), emptyList(), radonClass = 1, seismicZone = 1, sites = emptyList())

    @Test
    fun `should ask for the details at the commune's centre when the risks screen loads`() = runTest {
        val details = RiskDetails(ClayExposure.LOW, emptyList(), emptyList(), null)
        val risks = mockk<RiskRepository> {
            coEvery { risks("27681") } returns summary
            coEvery { riskDetails("27681", centre) } returns details
        }
        val viewModel = RisksViewModel("27681", communes, risks)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        assertEquals(RiskOverview(summary, details), viewModel.state.value.data.valueOrNull)
    }

    @Test
    fun `should still show the summary when the details fail`() = runTest {
        val risks = mockk<RiskRepository> {
            coEvery { risks("27681") } returns summary
            coEvery { riskDetails(any(), any()) } throws java.io.IOException("down")
        }
        val viewModel = RisksViewModel("27681", communes, risks)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        val overview = viewModel.state.value.data.valueOrNull!!
        assertEquals(summary, overview.risks)
        assertNull(overview.details)
    }
}
