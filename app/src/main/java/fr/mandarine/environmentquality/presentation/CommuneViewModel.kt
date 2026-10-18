package fr.mandarine.environmentquality.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.WaterSample
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.DroughtRepository
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.RecentRepository
import fr.mandarine.environmentquality.domain.repository.RiskRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import fr.mandarine.environmentquality.domain.usecase.BuildSampleCsvUseCase
import fr.mandarine.environmentquality.domain.usecase.FollowCommuneUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommuneUiState(
    val commune: LoadState<Commune> = LoadState.Loading,
    /** Loaded(null) when Hub'Eau has no sample for the commune. */
    val sample: LoadState<WaterSample?> = LoadState.Loading,
    val drought: LoadState<DroughtStatus> = LoadState.Loading,
    val risks: LoadState<CommuneRisks> = LoadState.Loading,
    val followed: Boolean = false,
)

data class CsvExport(val fileName: String, val content: String)

class CommuneViewModel(
    private val code: String,
    private val communes: CommuneRepository,
    private val water: WaterQualityRepository,
    private val drought: DroughtRepository,
    private val risks: RiskRepository,
    private val follows: FollowRepository,
    private val recent: RecentRepository,
    private val followCommune: FollowCommuneUseCase,
    private val buildCsv: BuildSampleCsvUseCase,
) : ViewModel() {

    private val content = MutableStateFlow(CommuneUiState())

    val state: StateFlow<CommuneUiState> = combine(content, follows.isFollowed(code)) { state, followed ->
        state.copy(followed = followed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommuneUiState())

    init {
        refresh()
    }

    fun refresh() {
        content.value = CommuneUiState()
        viewModelScope.launch {
            val commune = load { communes.byCode(code) ?: error("Unknown commune $code") }
            content.update { it.copy(commune = commune) }
            commune.valueOrNull?.let {
                recent.remember(it)
                follows.markSeen(code)
            }
        }
        viewModelScope.launch { content.update { it.copy(sample = load { water.latestSample(code) }) } }
        viewModelScope.launch { content.update { it.copy(drought = load { drought.status(code) }) } }
        viewModelScope.launch { content.update { it.copy(risks = load { risks.risks(code) }) } }
    }

    fun toggleFollow() {
        val commune = content.value.commune.valueOrNull ?: return
        viewModelScope.launch {
            if (state.value.followed) followCommune.unfollow(code) else followCommune.follow(commune)
        }
    }

    /** The CSV for the latest sample, or null while it is not loaded. */
    fun csv(): CsvExport? {
        val commune = content.value.commune.valueOrNull ?: return null
        val sample = content.value.sample.valueOrNull ?: return null
        return CsvExport(buildCsv.fileName(commune, sample), buildCsv(commune, sample))
    }
}
