package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.ParameterGroup
import fr.mandarine.environmentquality.domain.model.ParameterResult
import fr.mandarine.environmentquality.domain.model.WaterSample
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.presentation.SampleViewModel
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.ChipOption
import fr.mandarine.environmentquality.ui.components.ChoiceChips
import fr.mandarine.environmentquality.ui.components.EmptyText
import fr.mandarine.environmentquality.ui.components.ErrorBox
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.ScreenHeader
import fr.mandarine.environmentquality.ui.components.StatusMark
import fr.mandarine.environmentquality.ui.components.boundLabel
import fr.mandarine.environmentquality.ui.components.isFlagged
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.theme.Palette
import fr.mandarine.environmentquality.ui.theme.Sora

@Composable
fun ParametersScreen(code: String, navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel(key = "parameters-$code") { SampleViewModel(code, container.communes, container.water) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sample = state.data.valueOrNull
    val subtitle = listOfNotNull(
        state.commune.valueOrNull?.name,
        sample?.networks?.firstOrNull()?.let { stringResource(R.string.parameters_network, it.name.lowercase().replaceFirstChar(Char::titlecase)) },
    ).joinToString(" · ")
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.parameters_title), subtitle.ifEmpty { null }, navigator::back)
        when (val data = state.data) {
            LoadState.Loading -> LoadingBox()
            is LoadState.Failed -> ErrorBox(viewModel::refresh, Modifier.padding(16.dp))
            is LoadState.Loaded -> data.value?.let { ParameterList(it) }
                ?: EmptyText(stringResource(R.string.hero_no_sample), Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun groupLabel(group: ParameterGroup): String = stringResource(
    when (group) {
        ParameterGroup.NITRATES -> R.string.group_nitrates
        ParameterGroup.PHYSICAL -> R.string.group_physical
        ParameterGroup.CHLORINE -> R.string.group_chlorine
        ParameterGroup.SENSORY -> R.string.group_sensory
        ParameterGroup.BACTERIOLOGY -> R.string.group_bacteriology
        ParameterGroup.OTHER -> R.string.group_other
    },
)

@Composable
private fun ParameterList(sample: WaterSample) {
    var filter by rememberSaveable { mutableStateOf<ParameterGroup?>(null) }
    val groups = ParameterGroup.entries.filter { group -> sample.results.any { it.group == group } }
    val rows = sample.results
        .filter { filter == null || it.group == filter }
        .sortedWith(compareBy<ParameterResult>({ !it.isFlagged }, { it.group.ordinal }, { it.name }))
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.parameters_sampled), fontSize = 12.sp, color = Palette.InkSecondary, fontWeight = FontWeight.SemiBold)
                Text(Format.longDate(sample.sampledAt), style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            ChoiceChips(
                options = listOf(ChipOption<ParameterGroup?>(null, stringResource(R.string.group_all))) +
                    groups.map { ChipOption<ParameterGroup?>(it, groupLabel(it)) },
                selected = filter,
                onSelect = { filter = it },
            )
        }
        item {
            val flagged = rows.count { it.isFlagged }
            Text(
                if (rows.isEmpty()) {
                    stringResource(R.string.parameters_none)
                } else if (flagged == 0) {
                    pluralStringResource(R.plurals.parameters_count_ok, rows.size, rows.size)
                } else {
                    pluralStringResource(R.plurals.parameters_count_flagged, rows.size, rows.size, flagged)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(rows, key = { it.code }) { result ->
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    StatusMark(
                        when {
                            result.exceedsLimit -> WaterStatus.NON_CONFORM
                            result.isFlagged -> WaterStatus.RESERVES
                            else -> WaterStatus.CONFORM
                        },
                        size = 22.dp,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(result.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (result.numericValue != null) {
                                Spacer(Modifier.width(8.dp))
                                Text(result.displayValue, fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                result.unitLabel?.let {
                                    Spacer(Modifier.width(4.dp))
                                    Text(it, fontSize = 12.sp, color = Palette.InkSecondary)
                                }
                            }
                        }
                        // A qualitative result is a sentence, not a number: beside the name it
                        // squeezes the name into a narrow column, so it gets its own line.
                        if (result.numericValue == null) {
                            Text(result.displayValue, fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Text(boundLabel(result), fontSize = 12.sp, color = Palette.InkSecondary)
                        Text(groupLabel(result.group), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Palette.Water)
                    }
                }
            }
        }
    }
}
