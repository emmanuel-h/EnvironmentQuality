package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.ParameterResult
import fr.mandarine.environmentquality.domain.usecase.CommuneSide
import fr.mandarine.environmentquality.domain.usecase.Comparison
import fr.mandarine.environmentquality.presentation.CompareViewModel
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.ChipOption
import fr.mandarine.environmentquality.ui.components.ChoiceChips
import fr.mandarine.environmentquality.ui.components.ErrorBox
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.Gauge
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.ScreenHeader
import fr.mandarine.environmentquality.ui.components.StatusBadge
import fr.mandarine.environmentquality.ui.components.gaugeColor
import fr.mandarine.environmentquality.ui.components.prettyBound
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.theme.Palette
import fr.mandarine.environmentquality.ui.theme.Sora

@Composable
fun CompareScreen(code: String, navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel(key = "compare-$code") {
        CompareViewModel(code, container.communes, container.follows, container.recent, container.compareCommunes)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val name = state.commune.valueOrNull?.name
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.compare_title), name?.let { stringResource(R.string.compare_against, it) }, navigator::back)
        if (state.candidates.isNotEmpty()) {
            ChoiceChips(
                options = state.candidates.map { ChipOption(it.code, it.name) },
                selected = state.selected.orEmpty(),
                onSelect = { picked -> state.candidates.firstOrNull { it.code == picked }?.let(viewModel::select) },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp),
            )
        }
        when (val comparison = state.comparison) {
            LoadState.Loading -> LoadingBox()
            is LoadState.Failed -> ErrorBox(viewModel::retry, Modifier.padding(16.dp))
            is LoadState.Loaded -> CompareContent(comparison.value)
        }
    }
}

@Composable
private fun CompareContent(comparison: Comparison) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SideCard(comparison.left, Modifier.weight(1f))
            SideCard(comparison.right, Modifier.weight(1f))
        }
        AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            val left = comparison.left.commune.name
            val right = comparison.right.commune.name
            comparison.parameters.forEach { (a, b) ->
                ParameterRow(a, b, left, right)
                HorizontalDivider(color = Palette.Divider)
            }
            MonthsRow(comparison.left, comparison.right)
        }
        Text(stringResource(R.string.compare_note), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
    }
}

@Composable
private fun SideCard(side: CommuneSide, modifier: Modifier = Modifier) {
    AppCard(modifier, contentPadding = PaddingValues(14.dp)) {
        Text(
            side.commune.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val sample = side.sample
        if (sample == null) {
            Text(stringResource(R.string.compare_no_sample), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        } else {
            StatusBadge(sample.status, Modifier.padding(vertical = 8.dp))
            Text(Format.longDate(sample.sampledAt), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
        }
    }
}

@Composable
private fun ParameterRow(a: ParameterResult?, b: ParameterResult?, left: String, right: String) {
    val reference = a ?: b ?: return
    val bound = (reference.limitText ?: reference.referenceText)?.let(::prettyBound)
    val scale = listOfNotNull(reference.limit?.max ?: reference.reference?.max, a?.numericValue, b?.numericValue).maxOrNull()
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(reference.name, style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
            val caption = listOfNotNull(reference.unitLabel, bound).joinToString(" · ")
            if (caption.isNotEmpty()) Text(caption, fontSize = 12.sp, color = Palette.InkSecondary)
        }
        ValueLine(left, a?.displayValue, ratio(a?.numericValue, scale), a?.gaugeColor() ?: Palette.Water)
        ValueLine(right, b?.displayValue, ratio(b?.numericValue, scale), b?.gaugeColor() ?: Palette.Water)
    }
}

@Composable
private fun MonthsRow(left: CommuneSide, right: CommuneSide) {
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.compare_months), style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
            Text(stringResource(R.string.compare_months_body), fontSize = 12.sp, color = Palette.InkSecondary)
        }
        listOf(left, right).forEach { side ->
            val conform = side.conformMonths
            val measured = side.measuredMonths
            val value = if (conform != null && measured != null && measured > 0) "$conform / $measured" else null
            val share = if (value != null) conform!!.toDouble() / measured!! else null
            ValueLine(side.commune.name, value, share?.toFloat() ?: 0f, if (share != null && share < 1.0) Palette.Reserves else Palette.Conform)
        }
    }
}

private fun ratio(value: Double?, scale: Double?): Float =
    if (value == null || scale == null || scale <= 0) 0f else (value / scale).toFloat().coerceIn(0f, 1f)

@Composable
private fun ValueLine(name: String, value: String?, ratio: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            name,
            fontSize = 12.sp,
            color = Palette.InkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(84.dp),
        )
        Gauge(ratio, color, Modifier.weight(1f))
        Text(
            value ?: stringResource(R.string.compare_not_measured),
            fontSize = 13.sp,
            fontFamily = Sora,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
        )
    }
}
