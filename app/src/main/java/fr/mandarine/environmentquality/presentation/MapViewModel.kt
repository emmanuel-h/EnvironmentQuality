package fr.mandarine.environmentquality.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.LatestStatus
import fr.mandarine.environmentquality.domain.repository.RecentRepository
import fr.mandarine.environmentquality.domain.repository.StationRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import fr.mandarine.environmentquality.domain.usecase.FollowCommuneUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Instant

enum class MapLayer { WATER, RIVERS, GROUNDWATER, FLOWS }

data class CommuneMarker(val commune: Commune, val status: WaterStatus, val sampledAt: Instant?)

data class StationMarker(val code: String, val name: String, val detail: String?, val location: GeoPoint)

data class MapUiState(
    val layer: MapLayer = MapLayer.WATER,
    val communes: List<CommuneMarker> = emptyList(),
    val selected: CommuneMarker? = null,
    val selectedFollowed: Boolean = false,
    val stations: LoadState<List<StationMarker>> = LoadState.Loaded(emptyList()),
    val selectedStation: StationMarker? = null,
    /** The commune whose department the station layers show. */
    val stationArea: Commune? = null,
)

class MapViewModel(
    private val water: WaterQualityRepository,
    private val stations: StationRepository,
    private val follows: FollowRepository,
    private val recent: RecentRepository,
    private val followCommune: FollowCommuneUseCase,
) : ViewModel() {

    private val statuses = MutableStateFlow<Map<String, LatestStatus?>>(emptyMap())
    private val selection = MutableStateFlow(Selection())
    private var stationJob: Job? = null
    private val fetching = mutableSetOf<String>()
    private val limit = Semaphore(4)

    private data class Selection(
        val layer: MapLayer = MapLayer.WATER,
        val communeCode: String? = null,
        val stations: LoadState<List<StationMarker>> = LoadState.Loaded(emptyList()),
        val stationCode: String? = null,
        val stationArea: Commune? = null,
    )

    val state: StateFlow<MapUiState> = combine(
        follows.followed(),
        recent.recent(),
        statuses,
        selection,
    ) { followed, recents, known, sel ->
        val all = (recents + followed.map { it.commune } + ReferenceCommunes)
            .filter { it.centre != null }
            .distinctBy { it.code }
        all.forEach(::ensureStatus)
        val markers = all.map { commune ->
            val latest = known[commune.code]
            CommuneMarker(commune, latest?.flags?.status ?: WaterStatus.UNKNOWN, latest?.sampledAt)
        }
        val selectedCode = sel.communeCode ?: recents.firstOrNull()?.code ?: followed.firstOrNull()?.commune?.code
        MapUiState(
            layer = sel.layer,
            communes = markers,
            selected = markers.firstOrNull { it.commune.code == selectedCode },
            selectedFollowed = followed.any { it.commune.code == selectedCode },
            stations = sel.stations,
            selectedStation = sel.stations.valueOrNull?.firstOrNull { it.code == sel.stationCode },
            stationArea = sel.stationArea,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun selectCommune(code: String) {
        selection.value = selection.value.copy(communeCode = code)
    }

    fun selectStation(code: String) {
        selection.value = selection.value.copy(stationCode = code)
    }

    fun selectLayer(layer: MapLayer) {
        selection.value = selection.value.copy(layer = layer, stationCode = null)
        stationJob?.cancel()
        if (layer == MapLayer.WATER) return
        val area = state.value.selected?.commune ?: ReferenceCommunes.first()
        selection.value = selection.value.copy(stations = LoadState.Loading, stationArea = area)
        stationJob = viewModelScope.launch {
            val result = load { stationsFor(layer, area) }
            selection.value = selection.value.copy(stations = result)
        }
    }

    fun toggleFollow() {
        val current = state.value.selected?.commune ?: return
        viewModelScope.launch {
            if (follows.isFollowed(current.code).first()) followCommune.unfollow(current.code) else followCommune.follow(current)
        }
    }

    private suspend fun stationsFor(layer: MapLayer, area: Commune): List<StationMarker> {
        val department = area.departmentCode ?: area.code.take(2)
        return when (layer) {
            MapLayer.WATER -> emptyList()
            MapLayer.RIVERS -> stations.hydroStations(department).mapNotNull { s ->
                s.location?.let { StationMarker(s.code, s.name, s.river, it) }
            }
            MapLayer.GROUNDWATER -> stations.groundwaterStations(area.code, department).mapNotNull { s ->
                s.location?.let { StationMarker(s.code, s.commune ?: s.code, listOfNotNull(s.nature, s.waterBody).joinToString(" · ").ifEmpty { null }, it) }
            }
            MapLayer.FLOWS -> stations.flowStations(department).mapNotNull { s ->
                s.location?.let { StationMarker(s.code, s.name, s.river, it) }
            }
        }
    }

    private fun ensureStatus(commune: Commune) {
        synchronized(fetching) { if (!fetching.add(commune.code)) return }
        viewModelScope.launch {
            val latest = limit.withPermit { runCatching { water.latestStatus(commune.code) }.getOrNull() }
            statuses.value = statuses.value + (commune.code to latest)
        }
    }
}
