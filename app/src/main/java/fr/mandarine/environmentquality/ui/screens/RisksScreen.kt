package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.IndustrialSite
import fr.mandarine.environmentquality.domain.model.NaturalDisaster
import fr.mandarine.environmentquality.domain.model.RiskGroup
import fr.mandarine.environmentquality.domain.model.SevesoStatus
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.presentation.RisksViewModel
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.components.EmptyText
import fr.mandarine.environmentquality.ui.components.ErrorBox
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.ScreenHeader
import fr.mandarine.environmentquality.ui.components.SectionTitle
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.theme.Palette

private const val DISASTERS_SHOWN = 4
private const val SITES_SHOWN = 3

@Composable
fun RisksScreen(code: String, navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel(key = "risks-$code") { RisksViewModel(code, container.communes, container.risks) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.risks_title), state.commune.valueOrNull?.name, navigator::back)
        when (val data = state.data) {
            LoadState.Loading -> LoadingBox()
            is LoadState.Failed -> ErrorBox(viewModel::refresh, Modifier.padding(16.dp))
            is LoadState.Loaded -> RisksContent(data.value)
        }
    }
}

@Composable
private fun RisksContent(risks: CommuneRisks) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(
                AppIcons.Quake,
                stringResource(R.string.risks_seismic),
                risks.seismicZone?.let { stringResource(R.string.risks_seismic_zone, it) } ?: stringResource(R.string.risks_unknown),
                risks.seismicZone?.let { stringResource(R.string.risks_seismic_scale) },
                Modifier.weight(1f),
            )
            Tile(
                AppIcons.Radon,
                stringResource(R.string.risks_radon),
                risks.radonClass?.let { stringResource(R.string.risks_radon_class, it) } ?: stringResource(R.string.risks_unknown),
                risks.radonClass?.let { stringResource(R.string.risks_radon_scale) },
                Modifier.weight(1f),
            )
        }

        if (risks.groups.isEmpty()) {
            EmptyText(stringResource(R.string.risks_none_short))
        } else {
            SectionTitle(pluralStringResource(R.plurals.risks_count, risks.riskCount, risks.riskCount))
            AppCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
                risks.groups.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider(color = Palette.Divider)
                    RiskGroupRow(group)
                }
                if (!risks.hasForestFire) {
                    HorizontalDivider(color = Palette.Divider)
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Fire, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.risks_no_forest_fire), style = MaterialTheme.typography.bodyMedium, color = Palette.InkSecondary)
                    }
                }
            }
        }

        if (risks.disasters.isNotEmpty()) DisastersSection(risks.disasters)
        SitesSection(risks.sites)
        Text(stringResource(R.string.risks_source), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Tile(icon: ImageVector, title: String, value: String, scale: String?, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Palette.Water, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = Palette.InkSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
        if (scale != null) Text(scale, style = MaterialTheme.typography.bodySmall)
    }
}

/** GASPAR family codes: 11 floods, 12 ground movement, 13 earthquake, 15 forest fire, 21 industry, 24 transport. */
private fun RiskGroup.icon(): ImageVector = when (code.take(2)) {
    "11", "23" -> AppIcons.Flood
    "12", "14", "26" -> AppIcons.Terrain
    "13" -> AppIcons.Quake
    "15" -> AppIcons.Fire
    "16" -> AppIcons.Waves
    "18" -> AppIcons.Radon
    "21", "22" -> AppIcons.Factory
    "24" -> AppIcons.Truck
    else -> AppIcons.Warning
}

@Composable
private fun RiskGroupRow(group: RiskGroup) {
    var expanded by rememberSaveable(group.code) { mutableStateOf(false) }
    val expandable = group.details.isNotEmpty()
    val expandedLabel = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (expandable) {
                    Modifier
                        .clickable(role = Role.Button) { expanded = !expanded }
                        .semantics { stateDescription = expandedLabel }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Palette.WaterTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(group.icon(), contentDescription = null, tint = Palette.Water, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleSmall)
                if (expandable) {
                    Text(
                        pluralStringResource(R.plurals.risks_types, group.details.size, group.details.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (expandable) {
                Icon(
                    AppIcons.ChevronDown,
                    contentDescription = null,
                    tint = Palette.Muted,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f),
                )
            }
        }
        if (expanded) {
            Column(Modifier.padding(start = 44.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                group.details.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun DisastersSection(disasters: List<NaturalDisaster>) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    SectionTitle(
        pluralStringResource(R.plurals.risks_disasters, disasters.size, disasters.size),
        action = if (disasters.size > DISASTERS_SHOWN) {
            {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(
                        if (showAll) stringResource(R.string.show_less) else stringResource(R.string.show_all, disasters.size),
                        color = Palette.Water,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else {
            null
        },
    )
    val shown = disasters.sortedByDescending { it.start }.let { if (showAll) it else it.take(DISASTERS_SHOWN) }
    AppCard(Modifier.fillMaxWidth()) {
        shown.forEachIndexed { index, disaster ->
            if (index > 0) HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(vertical = 8.dp))
            Text(disaster.label, style = MaterialTheme.typography.titleSmall)
            val period = listOfNotNull(disaster.start, disaster.end).distinct().joinToString(" – ") { Format.mediumDate(it) }
            val published = disaster.published?.let { stringResource(R.string.risks_published, Format.mediumDate(it)) }
            val line = listOf(period, published.orEmpty()).filter { it.isNotEmpty() }.joinToString(" · ")
            if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SitesSection(sites: List<IndustrialSite>) {
    SectionTitle(stringResource(R.string.risks_sites))
    if (sites.isEmpty()) {
        EmptyText(stringResource(R.string.risks_no_site))
        return
    }
    val ordered = sites.sortedBy {
        when (it.seveso) {
            SevesoStatus.HIGH -> 0
            SevesoStatus.LOW -> 1
            else -> 2
        }
    }
    AppCard(Modifier.fillMaxWidth()) {
        ordered.take(SITES_SHOWN).forEachIndexed { index, site ->
            if (index > 0) HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Factory, contentDescription = null, tint = Palette.InkSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(site.name, style = MaterialTheme.typography.titleSmall)
                    val detail = listOfNotNull(site.activity, site.regime).joinToString(" · ")
                    if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall)
                }
                SevesoBadge(site.seveso)
            }
        }
        val others = ordered.size - SITES_SHOWN
        if (others > 0) {
            Spacer(Modifier.height(10.dp))
            Text(pluralStringResource(R.plurals.risks_other_sites, others, others), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SevesoBadge(status: SevesoStatus) {
    val label = when (status) {
        SevesoStatus.HIGH -> R.string.seveso_high
        SevesoStatus.LOW -> R.string.seveso_low
        else -> return
    }
    val high = status == SevesoStatus.HIGH
    Text(
        stringResource(label),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (high) Palette.NonConform else Palette.ReservesText,
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (high) Palette.NonConformBg else Palette.ReservesBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
