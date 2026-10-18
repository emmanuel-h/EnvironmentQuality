package fr.mandarine.environmentquality.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.RecentRepository
import fr.mandarine.environmentquality.domain.usecase.CompareCommunesUseCase
import fr.mandarine.environmentquality.domain.usecase.Comparison
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareUiState(
    val commune: LoadState<Commune> = LoadState.Loading,
    /** Communes offered for comparison: followed, then recent, then large cities. */
    val candidates: List<Commune> = emptyList(),
    val selected: String? = null,
    val comparison: LoadState<Comparison> = LoadState.Loading,
)

class CompareViewModel(
    private val code: String,
    private val communes: CommuneRepository,
    private val follows: FollowRepository,
    private val recent: RecentRepository,
    private val compare: CompareCommunesUseCase,
) : ViewModel() {

    private val content = MutableStateFlow(CompareUiState())
    val state: StateFlow<CompareUiState> = content.asStateFlow()

    private var comparing: Job? = null

    init {
        viewModelScope.launch {
            content.update { it.copy(commune = load { communes.byCode(code) ?: error("Unknown commune $code") }) }
            val candidates = candidates()
            content.update { it.copy(candidates = candidates) }
            candidates.firstOrNull()?.let(::select)
        }
    }

    fun select(other: Commune) {
        val commune = content.value.commune.valueOrNull
        content.update { it.copy(selected = other.code, comparison = LoadState.Loading) }
        if (commune == null) {
            content.update { it.copy(comparison = LoadState.Failed(IllegalStateException("Unknown commune $code"))) }
            return
        }
        comparing?.cancel()
        comparing = viewModelScope.launch {
            content.update { it.copy(comparison = load { compare(commune, other) }) }
        }
    }

    fun retry() {
        val selected = content.value.candidates.firstOrNull { it.code == content.value.selected }
        if (content.value.commune is LoadState.Failed || selected == null) {
            viewModelScope.launch {
                content.update { it.copy(commune = load { communes.byCode(code) ?: error("Unknown commune $code") }) }
                (selected ?: content.value.candidates.firstOrNull())?.let(::select)
            }
        } else {
            select(selected)
        }
    }

    private suspend fun candidates(): List<Commune> {
        val followed = runCatching { follows.followedNow().map { it.commune } }.getOrDefault(emptyList())
        val recents = runCatching { recent.recent().first() }.getOrDefault(emptyList())
        val own = (followed + recents).filter { it.code != code }.distinctBy { it.code }.take(MAX_CANDIDATES)
        val fill = ReferenceCommunes.filter { ref -> ref.code != code && own.none { it.code == ref.code } }
        return own + fill.take((MIN_CANDIDATES - own.size).coerceAtLeast(0))
    }

    private companion object {
        const val MAX_CANDIDATES = 6
        const val MIN_CANDIDATES = 3
    }
}
