package fr.mandarine.environmentquality.presentation

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.Conformity
import fr.mandarine.environmentquality.domain.model.ConformityFlags
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.WaterSample
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.DroughtRepository
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.RecentRepository
import fr.mandarine.environmentquality.domain.repository.RiskRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import fr.mandarine.environmentquality.domain.usecase.BuildSampleCsvUseCase
import fr.mandarine.environmentquality.domain.usecase.FollowCommuneUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CommuneViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val vernon = Commune("27681", "Vernon")
    private val sample = WaterSample(
        "S1", "27681", Instant.parse("2026-07-29T11:30:00Z"), emptyList(), null, null, null,
        ConformityFlags(Conformity.CONFORM, Conformity.CONFORM, Conformity.CONFORM, Conformity.CONFORM), emptyList(),
    )
    private val communes = mockk<CommuneRepository> { coEvery { byCode("27681") } returns vernon }
    private val water = mockk<WaterQualityRepository> { coEvery { latestSample("27681") } returns sample }
    private val drought = mockk<DroughtRepository> { coEvery { status("27681") } returns DroughtStatus(emptyList()) }
    private val risks = mockk<RiskRepository> { coEvery { risks("27681") } throws java.io.IOException("down") }
    private val follows = mockk<FollowRepository>(relaxed = true) { every { isFollowed("27681") } returns flowOf(false) }
    private val recent = mockk<RecentRepository>(relaxed = true)

    private fun viewModel() = CommuneViewModel(
        "27681", communes, water, drought, risks, follows, recent,
        FollowCommuneUseCase(follows, water), BuildSampleCsvUseCase(),
    )

    @Test
    fun `should load each section independently when one source fails`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        val state = viewModel.state.value
        assertEquals(vernon, state.commune.valueOrNull)
        assertEquals(sample, state.sample.valueOrNull)
        assertTrue(state.drought is LoadState.Loaded)
        assertTrue(state.risks is LoadState.Failed)
    }

    @Test
    fun `should remember the commune and clear its update flag when opened`() = runTest {
        viewModel()

        coVerify { recent.remember(vernon) }
        coVerify { follows.markSeen("27681") }
    }

    @Test
    fun `should build the csv once the sample is loaded`() = runTest {
        val export = viewModel().csv()

        assertEquals("eau-27681-20260729.csv", export?.fileName)
    }

    @Test
    fun `should not build a csv when the commune has no sample`() = runTest {
        coEvery { water.latestSample("27681") } returns null

        assertNull(viewModel().csv())
    }
}
