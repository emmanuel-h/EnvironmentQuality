package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.WaterAlert
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.SettingsRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository

/**
 * Looks for a new sample in each followed commune and returns the alerts the user
 * asked for. At most one alert per commune and per new sample, the most serious.
 */
class CheckFollowedCommunesUseCase(
    private val follows: FollowRepository,
    private val water: WaterQualityRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(): List<WaterAlert> {
        val prefs = settings.alertSettingsNow()
        val alerts = mutableListOf<WaterAlert>()
        for (followed in follows.followedNow()) {
            val latest = runCatching { water.latestStatus(followed.commune.code) }.getOrNull() ?: continue
            if (latest.sampleCode == followed.lastSampleCode) continue
            follows.recordCheck(followed.commune.code, latest, hasUpdate = true)
            val kind = when {
                latest.flags.status == WaterStatus.NON_CONFORM && prefs.nonConform -> WaterAlert.Kind.NON_CONFORM
                latest.flags.status == WaterStatus.RESERVES && prefs.referenceExceeded -> WaterAlert.Kind.REFERENCE_EXCEEDED
                prefs.newSample -> WaterAlert.Kind.NEW_SAMPLE
                else -> null
            } ?: continue
            val sample = runCatching { water.latestSample(followed.commune.code) }.getOrNull() ?: continue
            alerts += WaterAlert(followed.commune, kind, sample)
        }
        return alerts
    }
}
