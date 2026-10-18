package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository

/** Following stores the latest sample so the first check does not alert on old news. */
class FollowCommuneUseCase(
    private val follows: FollowRepository,
    private val water: WaterQualityRepository,
) {
    suspend fun follow(commune: Commune) {
        val latest = runCatching { water.latestStatus(commune.code) }.getOrNull()
        follows.follow(commune, latest)
    }

    suspend fun unfollow(code: String) = follows.unfollow(code)
}
