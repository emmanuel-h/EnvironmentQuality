package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.DailyTemperature
import fr.mandarine.environmentquality.domain.model.FlowObservation
import fr.mandarine.environmentquality.domain.model.FlowState
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GroundwaterResult
import fr.mandarine.environmentquality.domain.model.GroundwaterSampling
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
                FlowCard(nearby.flowStations, nearby.flows, nearby.flowStationCount)
            }
        }
        if (tab != NearbyTab.RIVERS) {
            SectionTitle(stringResource(R.string.nearby_groundwater))
            nearby.groundwater?.let { GroundwaterCard(it, nearby.groundwaterSampling) } ?: EmptyText(stringResource(R.string.nearby_no_groundwater))
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
            if (temperature.days.size >= 2) {
                Spacer(Modifier.height(12.dp))
                TemperatureDays(temperature.days)
            }
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

/** One bar per day, spanning that day's coolest to warmest reading. */
@Composable
private fun TemperatureDays(days: List<DailyTemperature>) {
    val low = days.minOf { it.minCelsius }
    val high = days.maxOf { it.maxCelsius }
    // A margin around the range so a flat day is still a visible bar.
    val bottom = low - 0.5
    val span = high + 0.5 - bottom
    val description = days.joinToString { day ->
        Format.mediumDate(day.date) + " " + Format.fixed(day.minCelsius, 1) + "–" + Format.fixed(day.maxCelsius, 1) + " °C"
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { contentDescription = description },
    ) {
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (days.size - 1)) / days.size).coerceAtLeast(2f)
        val minHeight = 3.dp.toPx()
        days.forEachIndexed { index, day ->
            val top = (size.height * (1 - (day.maxCelsius - bottom) / span)).toFloat()
            val base = (size.height * (1 - (day.minCelsius - bottom) / span)).toFloat()
            val h = (base - top).coerceAtLeast(minHeight)
            drawRoundRect(
                if (index == days.size - 1) Palette.Water else Palette.River,
                topLeft = Offset(index * (barWidth + gap), base - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(Format.mediumDate(days.first().date), fontSize = 11.sp, color = Palette.InkSecondary)
        Text(Format.mediumDate(days.last().date), fontSize = 11.sp, color = Palette.InkSecondary)
    }
    Text(
        stringResource(R.string.nearby_temperature_range, days.size, Format.fixed(low, 1), Format.fixed(high, 1)),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun FlowCard(stations: List<FlowStation>, flows: Map<String, FlowObservation>, total: Int) {
    AppCard(Modifier.fillMaxWidth()) {
        stations.forEachIndexed { index, station ->
            if (index > 0) HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(vertical = 8.dp))
            val observation = flows[station.code]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.river ?: station.name, style = MaterialTheme.typography.titleSmall)
                    station.commune?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    observation?.let {
                        Text(stringResource(R.string.nearby_flow_observed, Format.mediumDate(it.observedOn)), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (observation != null) FlowBadge(observation.state) else ActiveBadge(station.active)
            }
        }
        if (flows.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.nearby_flow_legend), style = MaterialTheme.typography.bodySmall)
        }
        if (total > stations.size) {
            Spacer(Modifier.height(10.dp))
            Text(pluralStringResource(R.plurals.nearby_flow_total, total, total), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActiveBadge(active: Boolean) {
    if (active) {
        Badge(stringResource(R.string.nearby_active), Palette.Conform, Palette.Conform, Palette.ConformBg)
    } else {
        Badge(stringResource(R.string.nearby_inactive), Palette.InkSecondary, Palette.Muted, Palette.UnknownBg)
    }
}

/** Each class has its own word, so the badge never relies on its colour alone. */
@Composable
private fun FlowBadge(state: FlowState) {
    when (state) {
        FlowState.FLOWING -> Badge(stringResource(R.string.flow_flowing), Palette.Conform, Palette.Conform, Palette.ConformBg)
        FlowState.VISIBLE -> Badge(stringResource(R.string.flow_visible), Palette.Conform, Palette.Conform, Palette.ConformBg)
        FlowState.LOW -> Badge(stringResource(R.string.flow_low), Palette.ReservesText, Palette.Reserves, Palette.ReservesBg)
        FlowState.NOT_VISIBLE -> Badge(stringResource(R.string.flow_not_visible), Palette.NonConform, Palette.NonConform, Palette.NonConformBg)
        FlowState.DRY -> Badge(stringResource(R.string.flow_dry), Palette.NonConform, Palette.NonConform, Palette.NonConformBg)
        FlowState.UNKNOWN -> Badge(stringResource(R.string.flow_unknown), Palette.InkSecondary, Palette.Muted, Palette.UnknownBg)
    }
}

@Composable
private fun Badge(label: String, text: Color, dot: Color, background: Color) {
    Row(
        Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = text)
    }
}

@Composable
private fun GroundwaterCard(station: GroundwaterStation, sampling: GroundwaterSampling?) {
    AppCard(Modifier.fillMaxWidth()) {
        StationHeader(AppIcons.Drop, station.commune ?: station.code, station.code)
        Spacer(Modifier.height(10.dp))
        station.nature?.let { Detail(stringResource(R.string.nearby_nature), it) }
        station.waterBody?.let { Detail(stringResource(R.string.nearby_water_body), it) }
        station.altitude?.let { Detail(stringResource(R.string.nearby_altitude), stringResource(R.string.unit_metres, Format.decimal(it, 0))) }
        HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(vertical = 12.dp))
        if (sampling == null) {
            Text(stringResource(R.string.nearby_groundwater_none), style = MaterialTheme.typography.bodyMedium, color = Palette.InkSecondary)
        } else {
            GroundwaterSamplingSummary(sampling)
        }
    }
}

@Composable
private fun GroundwaterSamplingSummary(sampling: GroundwaterSampling) {
    val nitrates = sampling.nitrates
    val pesticides = sampling.pesticides
    if (nitrates != null || pesticides != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            nitrates?.let {
                GroundwaterMeasure(stringResource(R.string.nearby_groundwater_nitrates), it, GroundwaterSampling.NITRATES_LIMIT, Modifier.weight(1f))
            }
            pesticides?.let {
                GroundwaterMeasure(stringResource(R.string.nearby_groundwater_pesticides), it, GroundwaterSampling.PESTICIDES_TOTAL_LIMIT, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    val count = sampling.results.size
    Text(
        pluralStringResource(R.plurals.nearby_groundwater_substances, count, count, sampling.quantifiedCount),
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.Clock, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.nearby_groundwater_sampled, Format.longDate(sampling.sampledOn)), style = MaterialTheme.typography.bodySmall)
    }
}

/** Sandre units name the measured form, "mg(NO3)/L"; the plain unit reads better next to a number. */
private fun plainUnit(unit: String?): String = unit?.replace(Regex("\\(.*?\\)"), "").orEmpty()

@Composable
private fun GroundwaterMeasure(label: String, result: GroundwaterResult, limit: Double, modifier: Modifier = Modifier) {
    val unit = plainUnit(result.unit)
    val limitText = listOf(Format.decimal(limit), unit).filter { it.isNotEmpty() }.joinToString(" ")
    val value = result.value
    val over = result.quantified && value != null && value > limit
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.InkSecondary)
        Text(
            if (result.quantified && value != null) {
                listOf(Format.decimal(value), unit).filter { it.isNotEmpty() }.joinToString(" ")
            } else {
                stringResource(R.string.nearby_groundwater_not_quantified)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = if (over) Palette.NonConform else Color.Unspecified,
        )
        Text(
            stringResource(if (over) R.string.nearby_groundwater_over else R.string.nearby_groundwater_limit, limitText),
            style = MaterialTheme.typography.bodySmall,
            color = if (over) Palette.NonConform else Color.Unspecified,
        )
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
