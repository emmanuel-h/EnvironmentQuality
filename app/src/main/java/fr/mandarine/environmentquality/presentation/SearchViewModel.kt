package fr.mandarine.environmentquality.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.LocationProvider
import fr.mandarine.environmentquality.domain.repository.RecentRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import fr.mandarine.environmentquality.domain.usecase.SearchCommunesUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecentCommune(val commune: Commune, val status: WaterStatus)

sealed interface LocateState {
    data object Idle : LocateState
    data object Locating : LocateState
    data object Failed : LocateState
    data class Found(val commune: Commune) : LocateState
}

data class SearchUiState(
    val query: String = "",
    /** Null while nothing searchable has been typed. */
    val results: LoadState<List<Commune>>? = null,
    val recent: List<RecentCommune> = emptyList(),
    val locate: LocateState = LocateState.Idle,
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val search: SearchCommunesUseCase,
    private val communes: CommuneRepository,
    private val water: WaterQualityRepository,
    private val location: LocationProvider,
    recent: RecentRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val locate = MutableStateFlow<LocateState>(LocateState.Idle)
    private val recentStatuses = MutableStateFlow<Map<String, WaterStatus>>(emptyMap())

    private val results = query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { text ->
            flow {
                val trimmed = text.trim()
                if (trimmed.length < SearchCommunesUseCase.MIN_LENGTH) {
                    emit(null)
                } else {
                    emit(LoadState.Loading)
                    emit(load { search(trimmed) })
                }
            }
        }

    val state: StateFlow<SearchUiState> = combine(query, results, recent.recent(), recentStatuses, locate) { q, r, recents, known, loc ->
        recents.forEach { commune -> if (commune.code !in known) fetchStatus(commune.code) }
        SearchUiState(
            query = q,
            results = if (q.trim().length < SearchCommunesUseCase.MIN_LENGTH) null else r ?: LoadState.Loading,
            recent = recents.map { RecentCommune(it, known[it.code] ?: WaterStatus.UNKNOWN) },
            locate = loc,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun locate() {
        locate.value = LocateState.Locating
        viewModelScope.launch {
            val commune = runCatching { location.current()?.let { communes.at(it.latitude, it.longitude) } }.getOrNull()
            locate.value = if (commune != null) LocateState.Found(commune) else LocateState.Failed
        }
    }

    fun locationDenied() {
        locate.value = LocateState.Failed
    }

    fun locateHandled() {
        locate.value = LocateState.Idle
    }

    private val requested = mutableSetOf<String>()

    private fun fetchStatus(code: String) {
        if (!requested.add(code)) return
        viewModelScope.launch {
            val status = runCatching { water.latestStatus(code)?.flags?.status }.getOrNull() ?: WaterStatus.UNKNOWN
            recentStatuses.value = recentStatuses.value + (code to status)
        }
    }
}
