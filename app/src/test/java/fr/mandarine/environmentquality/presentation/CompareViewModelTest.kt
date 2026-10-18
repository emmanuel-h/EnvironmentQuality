package fr.mandarine.environmentquality.presentation

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.FollowedCommune
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.RecentRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import fr.mandarine.environmentquality.domain.usecase.CompareCommunesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompareViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val vernon = Commune("27681", "Vernon")
    private val evreux = Commune("27229", "Evreux")
    private val rouen = Commune("76540", "Rouen")
    private val communes = mockk<CommuneRepository> { coEvery { byCode("27681") } returns vernon }
    private val water = mockk<WaterQualityRepository> {
        coEvery { latestSample(any()) } returns null
        coEvery { history(any(), any()) } throws java.io.IOException("down")
    }

    private fun followed(commune: Commune) = FollowedCommune(commune, null, WaterStatus.UNKNOWN, null, hasUpdate = false)

    @Test
    fun `should offer followed then recent communes without the compared one when building candidates`() = runTest {
        val follows = mockk<FollowRepository> { coEvery { followedNow() } returns listOf(followed(vernon), followed(evreux)) }
        val recent = mockk<RecentRepository> { every { recent() } returns flowOf(listOf(rouen, evreux)) }

        val viewModel = CompareViewModel("27681", communes, follows, recent, CompareCommunesUseCase(water))

        val state = viewModel.state.value
        assertEquals(listOf("27229", "76540"), state.candidates.take(2).map { it.code })
        assertEquals(3, state.candidates.size)
        assertTrue(state.candidates.none { it.code == "27681" })
        assertEquals("27229", state.selected)
        assertEquals("Evreux", state.comparison.valueOrNull?.right?.commune?.name)
    }

    @Test
    fun `should compare with the picked commune when a chip is selected`() = runTest {
        val follows = mockk<FollowRepository> { coEvery { followedNow() } returns emptyList() }
        val recent = mockk<RecentRepository> { every { recent() } returns flowOf(emptyList()) }
        val viewModel = CompareViewModel("27681", communes, follows, recent, CompareCommunesUseCase(water))

        viewModel.select(rouen)

        assertEquals("76540", viewModel.state.value.selected)
        assertEquals(rouen, viewModel.state.value.comparison.valueOrNull?.right?.commune)
    }
}
