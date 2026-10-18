package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.ParameterSeries
import fr.mandarine.environmentquality.domain.model.WaterHistory
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.presentation.HistoryViewModel
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.ChipOption
import fr.mandarine.environmentquality.ui.components.ChoiceChips
import fr.mandarine.environmentquality.ui.components.EmptyText
import fr.mandarine.environmentquality.ui.components.ErrorBox
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.ScreenHeader
import fr.mandarine.environmentquality.ui.components.SectionTitle
import fr.mandarine.environmentquality.ui.components.StatusMark
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.theme.Palette
import fr.mandarine.environmentquality.ui.theme.Sora

@Composable
fun HistoryScreen(code: String, navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel(key = "history-$code") { HistoryViewModel(code, container.communes, container.water) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history = state.data.valueOrNull
    val range = history?.months?.takeIf { it.isNotEmpty() }?.let {
        Format.month(it.first().month.atDay(1)) + " – " + Format.month(it.last().month.atDay(1))
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            stringResource(R.string.history_title),
            listOfNotNull(state.commune.valueOrNull?.name, range).joinToString(" · ").ifEmpty { null },
            navigator::back,
        )
        when (val data = state.data) {
            LoadState.Loading -> LoadingBox()
            is LoadState.Failed -> ErrorBox(viewModel::refresh, Modifier.padding(16.dp))
            is LoadState.Loaded -> HistoryContent(data.value)
        }
    }
}

@Composable
private fun HistoryContent(history: WaterHistory) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (history.months.isEmpty()) {
            EmptyText(stringResource(R.string.history_empty))
            return@Column
        }
        AppCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.history_monthly), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                history.months.forEach { month ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusMark(month.status, 20.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            Format.month(month.month.atDay(1), "MMMMM").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val reserves = history.months.count { it.status == WaterStatus.RESERVES }
            val nonConform = history.months.count { it.status == WaterStatus.NON_CONFORM }
            Text(
                buildString {
                    append(stringResource(R.string.history_summary, history.conformMonths, history.measuredMonths))
                    if (nonConform > 0) append(' ').append(stringResource(R.string.history_summary_non_conform, nonConform))
                    if (reserves > 0) append(' ').append(stringResource(R.string.history_summary_reserves, reserves))
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (history.series.isNotEmpty()) {
            var selected by rememberSaveable { mutableStateOf(history.series.first().code) }
            val series = history.series.firstOrNull { it.code == selected } ?: history.series.first()
            SectionTitle(stringResource(R.string.history_trend))
            ChoiceChips(history.series.map { ChipOption(it.code, it.name) }, series.code, { selected = it })
            AppCard(Modifier.fillMaxWidth()) {
                SeriesChart(series)
            }
        }
    }
}

@Composable
private fun SeriesChart(series: ParameterSeries) {
    val bound = series.limit?.max ?: series.reference?.max
    val peak = series.points.maxOf { it.value }
    val top = maxOf(peak, bound ?: 0.0) * 1.15
    val unit = series.unit?.takeUnless { it.equals("SANS OBJET", true) }.orEmpty()
    val last = series.points.last()
    Text(unit, fontSize = 12.sp, color = Palette.InkSecondary)
    Spacer(Modifier.height(8.dp))
    val description = series.points.joinToString { Format.month(it.date) + " " + Format.decimal(it.value) + " " + unit }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = description },
    ) {
        val count = series.points.size
        val gap = 6.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)
        series.points.forEachIndexed { index, point ->
            val h = (point.value / top * size.height).toFloat()
            val over = bound != null && point.value > bound
            val color = when {
                over -> Palette.Reserves
                index == count - 1 -> Palette.Water
                else -> Palette.River
            }
            drawRoundRect(
                color,
                topLeft = Offset(index * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
        if (bound != null) {
            val y = (size.height - bound / top * size.height).toFloat()
            drawLine(
                Palette.NonConform,
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(Format.month(series.points.first().date), fontSize = 11.sp, color = Palette.InkSecondary)
        Text(Format.month(last.date), fontSize = 11.sp, color = Palette.InkSecondary)
    }
    if (bound != null) {
        Text(
            stringResource(if (series.limit?.max != null) R.string.history_limit_line else R.string.history_reference_line, Format.decimal(bound), unit),
            fontSize = 12.sp,
            color = Palette.NonConform,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(Format.decimal(last.value) + " " + unit, fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    }
    Text(stringResource(R.string.history_last_value, Format.month(last.date, "MMMM yyyy")), style = MaterialTheme.typography.bodySmall)
}
