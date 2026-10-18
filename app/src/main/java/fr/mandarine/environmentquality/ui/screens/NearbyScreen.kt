package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GroundwaterStation
import fr.mandarine.environmentquality.presentation.LiveRiver
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.presentation.Nearby
import fr.mandarine.environmentquality.presentation.NearbyViewModel
import fr.mandarine.environmentquality.presentation.RiverTemperature
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.components.ChipOption
import fr.mandarine.environmentquality.ui.components.ChoiceChips
import fr.mandarine.environmentquality.ui.components.EmptyText
import fr.mandarine.environmentquality.ui.components.ErrorBox
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.ScreenHeader
import fr.mandarine.environmentquality.ui.components.SectionTitle
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.theme.Palette
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/** Older than this, a temperature reading is shown with a warning. */
private const val STALE_DAYS = 30L

private enum class NearbyTab { ALL, RIVERS, GROUNDWATER }

@Composable
fun NearbyScreen(code: String, navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel(key = "nearby-$code") { NearbyViewModel(code, container.communes, container.stations) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(NearbyTab.ALL) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.nearby_title), state.commune.valueOrNull?.name, navigator::back)
        ChoiceChips(
            options = listOf(
                ChipOption(NearbyTab.ALL, stringResource(R.string.nearby_tab_all)),
                ChipOption(NearbyTab.RIVERS, stringResource(R.string.nearby_tab_rivers)),
                ChipOption(NearbyTab.GROUNDWATER, stringResource(R.string.nearby_tab_groundwater)),
            ),
            selected = tab,
            onSelect = { tab = it },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp),
        )
        when (val data = state.data) {
            LoadState.Loading -> LoadingBox()
            is LoadState.Failed -> ErrorBox(viewModel::refresh, Modifier.padding(16.dp))
            is LoadState.Loaded -> NearbyContent(data.value, tab)
        }
    }
}

@Composable
private fun NearbyContent(nearby: Nearby, tab: NearbyTab) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (tab != NearbyTab.GROUNDWATER) {
            SectionTitle(stringResource(R.string.nearby_live_river))
            nearby.river?.let { RiverCard(it) } ?: EmptyText(stringResource(R.string.nearby_no_river))
            nearby.temperature?.let { TemperatureCard(it) }
            SectionTitle(stringResource(R.string.nearby_flows))
            if (nearby.flowStations.isEmpty()) {
                EmptyText(stringResource(R.string.nearby_no_flow))
            } else {
                FlowCard(nearby.flowStations, nearby.flowStationCount)
            }
        }
        if (tab != NearbyTab.RIVERS) {
            SectionTitle(stringResource(R.string.nearby_groundwater))
            nearby.groundwater?.let { GroundwaterCard(it) } ?: EmptyText(stringResource(R.string.nearby_no_groundwater))
        }
        Text(stringResource(R.string.nearby_source), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StationHeader(icon: ImageVector, name: String, detail: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.WaterTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Water, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            if (!detail.isNullOrEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Measure(label: String, value: String, change: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.InkSecondary)
        Text(value, style = MaterialTheme.typography.headlineSmall)
        if (change != null) Text(change, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RiverCard(river: LiveRiver) {
    val reading = river.reading
    val distance = river.distanceKm?.let { stringResource(R.string.nearby_distance, Format.decimal(it, 1)) }
    AppCard(Modifier.fillMaxWidth()) {
        StationHeader(AppIcons.Waves, river.station.name, listOfNotNull(river.station.river, distance).joinToString(" · "))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            reading.heightMetres?.let { height ->
                Measure(
                    stringResource(R.string.nearby_height),
                    stringResource(R.string.unit_metres, Format.fixed(height, 2)),
                    reading.heightChangeMillimetres?.let { signed(it, R.string.nearby_change_mm, R.string.nearby_stable) },
                    Modifier.weight(1f),
                )
            }
            reading.flowCubicMetres?.let { flow ->
                val previous = reading.previousFlowCubicMetres
                Measure(
                    stringResource(R.string.nearby_flow),
                    stringResource(R.string.unit_flow, Format.decimal(flow, 1)),
                    previous?.let { signed(flow - it, R.string.nearby_change_flow, R.string.nearby_stable, digits = 2) },
                    Modifier.weight(1f),
                )
            }
        }
        (reading.heightAt ?: reading.flowAt)?.let {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Clock, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.nearby_measured_at, Format.longDate(it), Format.time(it)), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun signed(delta: Double, format: Int, stable: Int, digits: Int = 0): String {
    val shown = Format.decimal(abs(delta), digits)
    if (shown == Format.decimal(0.0, digits)) return stringResource(stable)
    return stringResource(format, (if (delta > 0) "+" else "−") + shown)
}

@Composable
private fun TemperatureCard(temperature: RiverTemperature) {
    val reading = temperature.reading
    AppCard(Modifier.fillMaxWidth()) {
        StationHeader(
            AppIcons.Thermometer,
            temperature.station.name,
            listOfNotNull(temperature.station.river, pluralStringResource(R.plurals.nearby_temperature_stations, temperature.stationCount, temperature.stationCount))
                .joinToString(" · "),
        )
        Spacer(Modifier.height(12.dp))
        if (reading == null) {
            Text(stringResource(R.string.nearby_no_temperature), style = MaterialTheme.typography.bodyMedium, color = Palette.InkSecondary)
        } else {
            Measure(
                stringResource(R.string.nearby_temperature),
                stringResource(R.string.unit_celsius, Format.fixed(reading.celsius, 1)),
                stringResource(R.string.nearby_measured_on, Format.longDate(reading.measuredAt.toLocalDate())),
            )
            val age = Duration.between(reading.measuredAt, LocalDateTime.now(ZoneId.of("Europe/Paris"))).toDays()
            if (age > STALE_DAYS) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.ReservesBg)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.Warning, contentDescription = null, tint = Palette.ReservesText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        pluralStringResource(R.plurals.nearby_stale, age.toInt(), age.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.ReservesText,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowCard(stations: List<FlowStation>, total: Int) {
    AppCard(Modifier.fillMaxWidth()) {
        stations.forEachIndexed { index, station ->
            if (index > 0) HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.river ?: station.name, style = MaterialTheme.typography.titleSmall)
                    station.commune?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                ActiveBadge(station.active)
            }
        }
        if (total > stations.size) {
            Spacer(Modifier.height(10.dp))
            Text(pluralStringResource(R.plurals.nearby_flow_total, total, total), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActiveBadge(active: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Palette.ConformBg else Palette.UnknownBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (active) Palette.Conform else Palette.Muted),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            stringResource(if (active) R.string.nearby_active else R.string.nearby_inactive),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Palette.Conform else Palette.InkSecondary,
        )
    }
}

@Composable
private fun GroundwaterCard(station: GroundwaterStation) {
    AppCard(Modifier.fillMaxWidth()) {
        StationHeader(AppIcons.Drop, station.commune ?: station.code, station.code)
        Spacer(Modifier.height(10.dp))
        station.nature?.let { Detail(stringResource(R.string.nearby_nature), it) }
        station.waterBody?.let { Detail(stringResource(R.string.nearby_water_body), it) }
        station.altitude?.let { Detail(stringResource(R.string.nearby_altitude), stringResource(R.string.unit_metres, Format.decimal(it, 0))) }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
