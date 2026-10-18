package fr.mandarine.environmentquality.domain.repository

import fr.mandarine.environmentquality.domain.model.AlertSettings
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.FollowedCommune
import kotlinx.coroutines.flow.Flow

interface FollowRepository {
    fun followed(): Flow<List<FollowedCommune>>
    fun isFollowed(code: String): Flow<Boolean>
    suspend fun follow(commune: Commune, latest: LatestStatus?)
    suspend fun unfollow(code: String)
    suspend fun followedNow(): List<FollowedCommune>
    suspend fun recordCheck(code: String, latest: LatestStatus, hasUpdate: Boolean)
    suspend fun markSeen(code: String)
}

interface RecentRepository {
    fun recent(): Flow<List<Commune>>
    suspend fun remember(commune: Commune)
}

interface SettingsRepository {
    fun alertSettings(): Flow<AlertSettings>
    suspend fun alertSettingsNow(): AlertSettings
    suspend fun setAlertSettings(settings: AlertSettings)
}
