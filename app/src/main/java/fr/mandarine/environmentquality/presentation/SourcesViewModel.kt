package fr.mandarine.environmentquality.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The Sources tab only needs the date of the freshest published result. */
class SourcesViewModel(private val water: WaterQualityRepository) : ViewModel() {

    private val content = MutableStateFlow<LoadState<LocalDate?>>(LoadState.Loading)
    val latestPublication: StateFlow<LoadState<LocalDate?>> = content.asStateFlow()

    init {
        viewModelScope.launch { content.value = load { water.latestPublication() } }
    }
}
