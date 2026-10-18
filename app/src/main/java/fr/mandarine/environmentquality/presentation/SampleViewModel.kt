package fr.mandarine.environmentquality.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.WaterHistory
import fr.mandarine.environmentquality.domain.model.WaterSample
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A commune and one more piece of data about it, loaded side by side. */
data class CommuneData<T>(
    val commune: LoadState<Commune> = LoadState.Loading,
    val data: LoadState<T> = LoadState.Loading,
)

/** Base for detail screens: the commune name for the header plus their own data. */
abstract class CommuneDataViewModel<T>(
    private val code: String,
    private val communes: CommuneRepository,
) : ViewModel() {

    private val content = MutableStateFlow(CommuneData<T>())
    val state: StateFlow<CommuneData<T>> = content.asStateFlow()

    protected abstract suspend fun fetch(code: String): T

    fun refresh() {
        content.value = CommuneData()
        viewModelScope.launch { content.update { it.copy(commune = load { communes.byCode(code) ?: error("Unknown commune $code") }) } }
        viewModelScope.launch { content.update { it.copy(data = load { fetch(code) }) } }
    }
}

/** The parameters screen: the latest sample in full. */
class SampleViewModel(
    code: String,
    communes: CommuneRepository,
    private val water: WaterQualityRepository,
) : CommuneDataViewModel<WaterSample?>(code, communes) {
    init {
        refresh()
    }

    override suspend fun fetch(code: String): WaterSample? = water.latestSample(code)
}

/** The history screen: the twelve months up to the latest published sample, which lags by a few months. */
class HistoryViewModel(
    code: String,
    communes: CommuneRepository,
    private val water: WaterQualityRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : CommuneDataViewModel<WaterHistory>(code, communes) {
    init {
        refresh()
    }

    override suspend fun fetch(code: String): WaterHistory =
        water.history(code, today().minusMonths(14).withDayOfMonth(1))
}
