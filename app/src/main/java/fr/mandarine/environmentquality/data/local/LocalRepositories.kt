package fr.mandarine.environmentquality.data.local

import fr.mandarine.environmentquality.data.remote.CachedResponse
import fr.mandarine.environmentquality.data.remote.ResponseCache
import fr.mandarine.environmentquality.domain.model.AlertSettings
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.FollowedCommune
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.LatestStatus
import fr.mandarine.environmentquality.domain.repository.RecentRepository
import fr.mandarine.environmentquality.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

class RoomResponseCache(private val dao: CachedResponseDao) : ResponseCache {
    override suspend fun read(url: String): CachedResponse? =
        dao.get(url)?.let { CachedResponse(it.body, Instant.ofEpochMilli(it.fetchedAtMillis)) }

    override suspend fun write(url: String, response: CachedResponse) =
        dao.put(CachedResponseEntity(url, response.body, response.fetchedAt.toEpochMilli()))
}

class RoomFollowRepository(
    private val dao: FollowedCommuneDao,
    private val clock: Clock = Clock.systemUTC(),
) : FollowRepository {

    override fun followed(): Flow<List<FollowedCommune>> = dao.observeAll().map { rows -> rows.map(::toDomain) }

    override fun isFollowed(code: String): Flow<Boolean> = dao.observeIsFollowed(code)

    override suspend fun follow(commune: Commune, latest: LatestStatus?) {
        dao.put(
            FollowedCommuneEntity(
                code = commune.code,
                commune = CommuneColumns.of(commune),
                lastSampleCode = latest?.sampleCode,
                lastStatus = (latest?.flags?.status ?: WaterStatus.UNKNOWN).name,
                lastSampledAtMillis = latest?.sampledAt?.toEpochMilli(),
                hasUpdate = false,
                followedAtMillis = clock.millis(),
            ),
        )
    }

    override suspend fun unfollow(code: String) = dao.delete(code)

    override suspend fun followedNow(): List<FollowedCommune> = dao.all().map(::toDomain)

    override suspend fun recordCheck(code: String, latest: LatestStatus, hasUpdate: Boolean) {
        val row = dao.get(code) ?: return
        dao.put(
            row.copy(
                lastSampleCode = latest.sampleCode,
                lastStatus = latest.flags.status.name,
                lastSampledAtMillis = latest.sampledAt.toEpochMilli(),
                hasUpdate = hasUpdate,
            ),
        )
    }

    override suspend fun markSeen(code: String) = dao.markSeen(code)

    private fun toDomain(row: FollowedCommuneEntity) = FollowedCommune(
        commune = row.commune.toCommune(),
        lastSampleCode = row.lastSampleCode,
        lastStatus = runCatching { WaterStatus.valueOf(row.lastStatus) }.getOrDefault(WaterStatus.UNKNOWN),
        lastSampledAt = row.lastSampledAtMillis?.let(Instant::ofEpochMilli),
        hasUpdate = row.hasUpdate,
    )
}

class RoomRecentRepository(
    private val dao: RecentCommuneDao,
    private val clock: Clock = Clock.systemUTC(),
) : RecentRepository {

    override fun recent(): Flow<List<Commune>> =
        dao.observeLatest(KEEP).map { rows -> rows.map { it.commune.toCommune() } }

    override suspend fun remember(commune: Commune) {
        dao.put(RecentCommuneEntity(commune.code, CommuneColumns.of(commune), clock.millis()))
        dao.trim(KEEP)
    }

    private companion object {
        const val KEEP = 8
    }
}

class RoomSettingsRepository(private val dao: AlertSettingsDao) : SettingsRepository {

    override fun alertSettings(): Flow<AlertSettings> = dao.observe().map { it?.toDomain() ?: AlertSettings() }

    override suspend fun alertSettingsNow(): AlertSettings = dao.get()?.toDomain() ?: AlertSettings()

    override suspend fun setAlertSettings(settings: AlertSettings) =
        dao.put(AlertSettingsEntity(0, settings.nonConform, settings.newSample, settings.referenceExceeded))

    private fun AlertSettingsEntity.toDomain() = AlertSettings(nonConform, newSample, referenceExceeded)
}
