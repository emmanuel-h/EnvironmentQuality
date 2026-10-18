package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.presentation.MapLayer
import fr.mandarine.environmentquality.presentation.MapUiState
import fr.mandarine.environmentquality.presentation.MapViewModel
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.components.ChipOption
import fr.mandarine.environmentquality.ui.components.ChoiceChips
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.FranceMap
import fr.mandarine.environmentquality.ui.components.MapPin
import fr.mandarine.environmentquality.ui.components.OutlinedIconButton
import fr.mandarine.environmentquality.ui.components.PrimaryButton
import fr.mandarine.environmentquality.ui.components.RoundIconButton
import fr.mandarine.environmentquality.ui.components.StatusBadge
import fr.mandarine.environmentquality.ui.components.StatusMark
import fr.mandarine.environmentquality.ui.navigation.CommuneKey
import fr.mandarine.environmentquality.ui.navigation.NearbyKey
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.navigation.SourcesKey
import fr.mandarine.environmentquality.ui.theme.Palette

@Composable
fun MapScreen(navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel {
        MapViewModel(container.water, container.stations, container.follows, container.recent, container.followCommune)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    MapContent(
        state = state,
        onLayer = viewModel::selectLayer,
        onCommune = viewModel::selectCommune,
        onStation = viewModel::selectStation,
        onToggleFollow = viewModel::toggleFollow,
        onOpenCommune = { navigator.open(CommuneKey(it)) },
        onOpenNearby = { navigator.open(NearbyKey(it)) },
        onSearch = { navigator.startSearch() },
        onSources = { navigator.selectTab(SourcesKey) },
    )
}

@Composable
internal fun MapContent(
    state: MapUiState,
    onLayer: (MapLayer) -> Unit,
    onCommune: (String) -> Unit,
    onStation: (String) -> Unit,
    onToggleFollow: () -> Unit,
    onOpenCommune: (String) -> Unit,
    onOpenNearby: (String) -> Unit,
    onSearch: () -> Unit,
    onSources: () -> Unit,
) {
    val water = state.layer == MapLayer.WATER
    val pins = if (water) {
        state.communes.mapNotNull { marker ->
            marker.commune.centre?.let { MapPin(marker.commune.code, it, marker.status, marker.commune.code == state.selected?.commune?.code) }
        }
    } else {
        state.stations.valueOrNull.orEmpty().map { MapPin(it.code, it.location, null, it.code == state.selectedStation?.code) }
    }
    Box(Modifier.fillMaxSize()) {
        FranceMap(
            pins = pins,
            onPinTap = if (water) onCommune else onStation,
            description = stringResource(R.string.map_description),
            topInset = 150.dp,
            bottomInset = 190.dp,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Drop, contentDescription = null, tint = Palette.Water, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                RoundIconButton(AppIcons.Info, stringResource(R.string.map_sources), onSources)
            }
            Surface(
                onClick = onSearch,
                shape = RoundedCornerShape(16.dp),
                color = Palette.Surface,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Search, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.search_hint), color = Palette.InkSecondary, fontSize = 15.sp)
                }
            }
            ChoiceChips(
                options = listOf(
                    ChipOption(MapLayer.WATER, stringResource(R.string.layer_water)),
                    ChipOption(MapLayer.RIVERS, stringResource(R.string.layer_rivers)),
                    ChipOption(MapLayer.GROUNDWATER, stringResource(R.string.layer_groundwater)),
                    ChipOption(MapLayer.FLOWS, stringResource(R.string.layer_flows)),
                ),
                selected = state.layer,
                onSelect = onLayer,
            )
        }
        MapSheet(state, onToggleFollow, onOpenCommune, onOpenNearby, onSearch, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MapSheet(
    state: MapUiState,
    onToggleFollow: () -> Unit,
    onOpenCommune: (String) -> Unit,
    onOpenNearby: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Palette.Surface)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Palette.Handle),
        )
        if (state.layer == MapLayer.WATER) {
            val selected = state.selected
            if (selected == null) {
                Text(stringResource(R.string.map_pick_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.map_pick_body), style = MaterialTheme.typography.bodySmall)
                PrimaryButton(stringResource(R.string.map_pick_action), onSearch, Modifier.fillMaxWidth())
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(selected.commune.name, style = MaterialTheme.typography.headlineSmall)
                        val sampled = selected.sampledAt?.let { stringResource(R.string.map_sampled_on, Format.longDate(it)) }
                        Text(
                            listOfNotNull(selected.commune.code, selected.commune.departmentName, sampled).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    StatusBadge(selected.status)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        stringResource(R.string.map_open_commune),
                        { onOpenCommune(selected.commune.code) },
                        Modifier.weight(1f),
                        trailing = true,
                    )
                    OutlinedIconButton(
                        icon = AppIcons.Bell,
                        contentDescription = stringResource(if (state.selectedFollowed) R.string.action_unfollow else R.string.action_follow),
                        onClick = onToggleFollow,
                        filled = state.selectedFollowed,
                    )
                }
                Legend()
            }
        } else {
            val (title, body) = when (state.layer) {
                MapLayer.RIVERS -> R.string.layer_rivers_title to R.string.layer_rivers_body
                MapLayer.GROUNDWATER -> R.string.layer_groundwater_title to R.string.layer_groundwater_body
                else -> R.string.layer_flows_title to R.string.layer_flows_body
            }
            val station = state.selectedStation
            Text(station?.name ?: stringResource(title), style = MaterialTheme.typography.headlineSmall)
            val area = state.stationArea
            val subtitle = when (val stations = state.stations) {
                LoadState.Loading -> stringResource(R.string.loading)
                is LoadState.Failed -> stringResource(R.string.error_generic)
                is LoadState.Loaded -> station?.detail
                    ?: if (stations.value.isEmpty()) stringResource(R.string.map_no_station) else stringResource(body, stations.value.size, area?.departmentName ?: "")
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            if (area != null) {
                PrimaryButton(stringResource(R.string.map_open_nearby, area.name), { onOpenNearby(area.code) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(WaterStatus.CONFORM, WaterStatus.RESERVES, WaterStatus.NON_CONFORM).forEach { status ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusMark(status, 12.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(
                        when (status) {
                            WaterStatus.CONFORM -> R.string.status_conform
                            WaterStatus.RESERVES -> R.string.status_reserves
                            else -> R.string.status_non_conform
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
