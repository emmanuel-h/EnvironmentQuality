package fr.mandarine.environmentquality.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mandarine.environmentquality.domain.model.AlertSettings
import fr.mandarine.environmentquality.domain.model.FollowedCommune
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FollowsUiState(
    /** Null until the database has answered, so the empty state never flashes. */
    val followed: List<FollowedCommune>? = null,
    val alerts: AlertSettings = AlertSettings(),
)

class FollowsViewModel(
    follows: FollowRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<FollowsUiState> = combine(follows.followed(), settings.alertSettings()) { followed, alerts ->
        FollowsUiState(followed, alerts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FollowsUiState())

    fun setAlerts(alerts: AlertSettings) {
        viewModelScope.launch { settings.setAlertSettings(alerts) }
    }
}
