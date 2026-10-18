package fr.mandarine.environmentquality

import android.content.Context
import fr.mandarine.environmentquality.data.device.AndroidLocationProvider
import fr.mandarine.environmentquality.data.local.AppDatabase
import fr.mandarine.environmentquality.data.local.RoomFollowRepository
import fr.mandarine.environmentquality.data.local.RoomRecentRepository
import fr.mandarine.environmentquality.data.local.RoomResponseCache
import fr.mandarine.environmentquality.data.local.RoomSettingsRepository
import fr.mandarine.environmentquality.data.remote.CachingHttpClient
import fr.mandarine.environmentquality.data.remote.UrlConnectionHttpClient
import fr.mandarine.environmentquality.data.repository.GeoCommuneRepository
import fr.mandarine.environmentquality.data.repository.GeorisquesRiskRepository
import fr.mandarine.environmentquality.data.repository.HubEauStationRepository
import fr.mandarine.environmentquality.data.repository.HubEauWaterQualityRepository
import fr.mandarine.environmentquality.data.repository.VigieauDroughtRepository
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.DroughtRepository
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.LocationProvider
import fr.mandarine.environmentquality.domain.repository.RecentRepository
import fr.mandarine.environmentquality.domain.repository.RiskRepository
import fr.mandarine.environmentquality.domain.repository.SettingsRepository
import fr.mandarine.environmentquality.domain.repository.StationRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import fr.mandarine.environmentquality.domain.usecase.BuildSampleCsvUseCase
import fr.mandarine.environmentquality.domain.usecase.CheckFollowedCommunesUseCase
import fr.mandarine.environmentquality.domain.usecase.CompareCommunesUseCase
import fr.mandarine.environmentquality.domain.usecase.FollowCommuneUseCase
import fr.mandarine.environmentquality.domain.usecase.SearchCommunesUseCase

/** Manual dependency wiring: one instance of each repository for the process. */
class AppContainer(context: Context) {

    private val database = AppDatabase.build(context)
    private val http = CachingHttpClient(
        network = UrlConnectionHttpClient(userAgent = "EnvironmentQuality (Android; open source)"),
        cache = RoomResponseCache(database.cachedResponses()),
    )

    val communes: CommuneRepository = GeoCommuneRepository(http)
    val water: WaterQualityRepository = HubEauWaterQualityRepository(http)
    val drought: DroughtRepository = VigieauDroughtRepository(http)
    val risks: RiskRepository = GeorisquesRiskRepository(http)
    val stations: StationRepository = HubEauStationRepository(http)
    val follows: FollowRepository = RoomFollowRepository(database.followedCommunes())
    val recent: RecentRepository = RoomRecentRepository(database.recentCommunes())
    val settings: SettingsRepository = RoomSettingsRepository(database.alertSettings())
    val location: LocationProvider = AndroidLocationProvider(context.applicationContext)

    val searchCommunes = SearchCommunesUseCase(communes)
    val followCommune = FollowCommuneUseCase(follows, water)
    val checkFollowed = CheckFollowedCommunesUseCase(follows, water, settings)
    val compareCommunes = CompareCommunesUseCase(water)
    val buildSampleCsv = BuildSampleCsvUseCase()
}
