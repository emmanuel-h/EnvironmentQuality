package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.AlertSettings
import fr.mandarine.environmentquality.domain.model.Conformity
import fr.mandarine.environmentquality.domain.model.ConformityFlags
import fr.mandarine.environmentquality.domain.model.FollowedCommune
import fr.mandarine.environmentquality.domain.model.WaterAlert
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.LatestStatus
import fr.mandarine.environmentquality.domain.repository.SettingsRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class CheckFollowedCommunesUseCaseTest {

    private val follows = mockk<FollowRepository>(relaxed = true)
    private val water = mockk<WaterQualityRepository>()
    private val settings = mockk<SettingsRepository>()
    private val check = CheckFollowedCommunesUseCase(follows, water, settings)

    private val conform = ConformityFlags(Conformity.CONFORM, Conformity.CONFORM, Conformity.CONFORM, Conformity.CONFORM)
    private val breached = conform.copy(physicoChemicalLimits = Conformity.NON_CONFORM)

    @Before
    fun setUp() {
        coEvery { settings.alertSettingsNow() } returns AlertSettings()
        coEvery { follows.followedNow() } returns listOf(
            FollowedCommune(vernon, "old", WaterStatus.CONFORM, Instant.EPOCH, hasUpdate = false),
        )
    }

    @Test
    fun `should not alert when the latest sample is the one already seen`() = runTest {
        coEvery { water.latestStatus("27681") } returns LatestStatus("old", Instant.EPOCH, conform)

        assertTrue(check().isEmpty())
        coVerify(exactly = 0) { follows.recordCheck(any(), any(), any()) }
    }

    @Test
    fun `should raise a non conform alert when a new sample breaches a limit`() = runTest {
        val latest = LatestStatus("new", Instant.EPOCH, breached)
        coEvery { water.latestStatus("27681") } returns latest
        coEvery { water.latestSample("27681") } returns sample(code = "new", flags = breached)

        val alerts = check()

        assertEquals(WaterAlert.Kind.NON_CONFORM, alerts.single().kind)
        coVerify { follows.recordCheck("27681", latest, true) }
    }

    @Test
    fun `should record the sample without alerting when the user turned new sample alerts off`() = runTest {
        coEvery { settings.alertSettingsNow() } returns AlertSettings(newSample = false)
        coEvery { water.latestStatus("27681") } returns LatestStatus("new", Instant.EPOCH, conform)

        assertTrue(check().isEmpty())
        coVerify { follows.recordCheck("27681", any(), true) }
    }

    @Test
    fun `should skip a commune when the api fails`() = runTest {
        coEvery { water.latestStatus("27681") } throws java.io.IOException("offline")

        assertTrue(check().isEmpty())
    }
}
