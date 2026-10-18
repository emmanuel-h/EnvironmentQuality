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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.Audience
import fr.mandarine.environmentquality.domain.model.DroughtLevel
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.WaterResource
import fr.mandarine.environmentquality.presentation.DroughtViewModel
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.components.ChipOption
import fr.mandarine.environmentquality.ui.components.ChoiceChips
import fr.mandarine.environmentquality.ui.components.DroughtBadge
import fr.mandarine.environmentquality.ui.components.EmptyText
import fr.mandarine.environmentquality.ui.components.ErrorBox
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.ScreenHeader
import fr.mandarine.environmentquality.ui.components.SectionTitle
import fr.mandarine.environmentquality.ui.components.droughtAdviceNoDate
import fr.mandarine.environmentquality.ui.components.look
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.theme.Palette

@Composable
fun DroughtScreen(code: String, navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel(key = "drought-$code") { DroughtViewModel(code, container.communes, container.drought) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zone = state.data.valueOrNull?.worstZone?.name?.lowercase()?.replaceFirstChar(Char::titlecase)
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            stringResource(R.string.drought_title),
            listOfNotNull(state.commune.valueOrNull?.name, zone?.let { stringResource(R.string.drought_zone, it) }).joinToString(" · ").ifEmpty { null },
            navigator::back,
        )
        when (val data = state.data) {
            LoadState.Loading -> LoadingBox()
            is LoadState.Failed -> ErrorBox(viewModel::refresh, Modifier.padding(16.dp))
            is LoadState.Loaded -> DroughtContent(data.value)
        }
    }
}

@Composable
private fun DroughtContent(status: DroughtStatus) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val level = status.level
        AppCard(Modifier.fillMaxWidth()) {
            DroughtBadge(level)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(if (level == DroughtLevel.NONE) R.string.drought_none_short else droughtAdviceNoDate(level)),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(14.dp))
            LevelScale(level)
            val zone = status.worstZone
            if (zone?.validFrom != null && zone.validUntil != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.drought_decree, Format.mediumDate(zone.validFrom!!), Format.mediumDate(zone.validUntil!!)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            zone?.decreeUrl?.let { url ->
                TextButton(onClick = { runCatching { uriHandler.openUri(url) } }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Icon(AppIcons.External, contentDescription = null, tint = Palette.Water, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.drought_read_decree), color = Palette.Water, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (status.zones.isEmpty()) {
            EmptyText(stringResource(R.string.drought_none_body))
        } else {
            SectionTitle(stringResource(R.string.drought_resources))
            AppCard(Modifier.fillMaxWidth()) {
                status.zones.forEachIndexed { index, zone ->
                    if (index > 0) HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(
                                when (zone.resource) {
                                    WaterResource.GROUNDWATER -> R.string.resource_groundwater
                                    WaterResource.SURFACE -> R.string.resource_surface
                                    WaterResource.DRINKING_WATER -> R.string.resource_drinking
                                    null -> R.string.resource_other
                                },
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        DroughtBadge(zone.level)
                    }
                }
            }
            UsesSection(status)
        }
        Text(stringResource(R.string.drought_source), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LevelScale(level: DroughtLevel) {
    val levels = listOf(DroughtLevel.VIGILANCE, DroughtLevel.ALERT, DroughtLevel.REINFORCED_ALERT, DroughtLevel.CRISIS)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        levels.forEach { step ->
            val active = step == level
            val reached = level >= step
            val look = step.look
            Column(
                Modifier
                    .weight(1f)
                    .semantics { if (active) stateDescription = "●" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (active) 10.dp else 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (reached) look.text else look.background),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(look.label),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Palette.Ink else Palette.InkSecondary,
                )
            }
        }
    }
}

@Composable
private fun UsesSection(status: DroughtStatus) {
    var audience by rememberSaveable { mutableStateOf(Audience.INDIVIDUALS) }
    SectionTitle(stringResource(R.string.drought_uses))
    ChoiceChips(
        options = listOf(
            ChipOption(Audience.INDIVIDUALS, stringResource(R.string.audience_individuals)),
            ChipOption(Audience.COMMUNITIES, stringResource(R.string.audience_communities)),
            ChipOption(Audience.FARMS, stringResource(R.string.audience_farms)),
            ChipOption(Audience.COMPANIES, stringResource(R.string.audience_companies)),
        ),
        selected = audience,
        onSelect = { audience = it },
    )
    val uses = status.usesFor(audience)
    AppCard(Modifier.fillMaxWidth()) {
        if (uses.isEmpty()) Text(stringResource(R.string.drought_no_use), style = MaterialTheme.typography.bodyMedium, color = Palette.InkSecondary)
        uses.forEachIndexed { index, use ->
            if (index > 0) HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(vertical = 10.dp))
            Text(use.name, style = MaterialTheme.typography.titleSmall)
            if (use.rule.isNotEmpty()) Text(use.rule, style = MaterialTheme.typography.bodySmall)
        }
    }
}
