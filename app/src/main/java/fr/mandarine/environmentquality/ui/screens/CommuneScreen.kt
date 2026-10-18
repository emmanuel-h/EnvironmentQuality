package fr.mandarine.environmentquality.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.Conformity
import fr.mandarine.environmentquality.domain.model.DroughtLevel
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.WaterSample
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.usecase.keyParameters
import fr.mandarine.environmentquality.presentation.CommuneUiState
import fr.mandarine.environmentquality.presentation.CommuneViewModel
import fr.mandarine.environmentquality.presentation.CsvExport
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.components.ErrorBox
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.Gauge
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.RoundIconButton
import fr.mandarine.environmentquality.ui.components.SectionTitle
import fr.mandarine.environmentquality.ui.components.DroughtBadge
import fr.mandarine.environmentquality.ui.components.boundLabel
import fr.mandarine.environmentquality.ui.components.droughtAdvice
import fr.mandarine.environmentquality.ui.components.droughtAdviceNoDate
import fr.mandarine.environmentquality.ui.components.gaugeColor
import fr.mandarine.environmentquality.ui.components.look
import fr.mandarine.environmentquality.ui.components.rememberNotificationPermissionRequest
import fr.mandarine.environmentquality.ui.navigation.CompareKey
import fr.mandarine.environmentquality.ui.navigation.DroughtKey
import fr.mandarine.environmentquality.ui.navigation.HistoryKey
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.navigation.NearbyKey
import fr.mandarine.environmentquality.ui.navigation.ParametersKey
import fr.mandarine.environmentquality.ui.navigation.RisksKey
import fr.mandarine.environmentquality.ui.theme.Palette
import fr.mandarine.environmentquality.ui.theme.Sora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CommuneScreen(code: String, navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel(key = "commune-$code") {
        CommuneViewModel(
            code, container.communes, container.water, container.drought, container.risks,
            container.follows, container.recent, container.followCommune, container.buildSampleCsv,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = remember { mutableStateOf<CsvExport?>(null) }
    val saved = stringResource(R.string.csv_saved)
    val failed = stringResource(R.string.csv_failed)
    val requestNotifications = rememberNotificationPermissionRequest()
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val export = pending.value ?: return@rememberLauncherForActivityResult
        pending.value = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(export.content.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            Toast.makeText(context, if (ok) saved else failed, Toast.LENGTH_SHORT).show()
        }
    }
    CommuneContent(
        state = state,
        onBack = navigator::back,
        onRetry = viewModel::refresh,
        onToggleFollow = {
            if (!state.followed) requestNotifications()
            viewModel.toggleFollow()
        },
        onParameters = { navigator.open(ParametersKey(code)) },
        onHistory = { navigator.open(HistoryKey(code)) },
        onNearby = { navigator.open(NearbyKey(code)) },
        onCompare = { navigator.open(CompareKey(code)) },
        onDrought = { navigator.open(DroughtKey(code)) },
        onRisks = { navigator.open(RisksKey(code)) },
        onExport = {
            viewModel.csv()?.let { export ->
                pending.value = export
                createFile.launch(export.fileName)
            }
        },
    )
}

@Composable
internal fun CommuneContent(
    state: CommuneUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFollow: () -> Unit,
    onParameters: () -> Unit,
    onHistory: () -> Unit,
    onNearby: () -> Unit,
    onCompare: () -> Unit,
    onDrought: () -> Unit,
    onRisks: () -> Unit,
    onExport: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoundIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack)
            FollowButton(state.followed, onToggleFollow)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (val commune = state.commune) {
                LoadState.Loading -> LoadingBox()
                is LoadState.Failed -> ErrorBox(onRetry)
                is LoadState.Loaded -> CommuneTitle(commune.value)
            }
            when (val sample = state.sample) {
                LoadState.Loading -> LoadingBox()
                is LoadState.Failed -> ErrorBox(onRetry)
                is LoadState.Loaded -> {
                    StatusHero(sample.value)
                    sample.value?.let { value ->
                        ConformityGrid(value)
                        KeyParameters(value, onParameters)
                    }
                }
            }
            SectionTitle(stringResource(R.string.commune_environment))
            DroughtTile(state.drought, onDrought)
            RisksTile(state.risks, onRisks)
            ShortcutGrid(onHistory, onNearby, onCompare, onExport, exportEnabled = state.sample.valueOrNull != null)
            state.sample.valueOrNull?.let { NetworkCard(it) }
        }
    }
}

@Composable
private fun FollowButton(followed: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (followed) Palette.Water else Palette.Surface,
        shadowElevation = 2.dp,
        modifier = Modifier.height(44.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            val tint = if (followed) Color.White else Palette.Ink
            Icon(AppIcons.Star, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (followed) R.string.commune_followed else R.string.commune_follow),
                color = tint,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun CommuneTitle(commune: Commune) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(commune.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        val population = commune.population?.let { stringResource(R.string.population, Format.integer(it)) }
        val area = listOfNotNull(commune.departmentName, commune.regionName).joinToString(", ").ifEmpty { null }
        Text(listOfNotNull(commune.code, area, population).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Palette.InkSecondary)
    }
}

@Composable
private fun StatusHero(sample: WaterSample?) {
    val status = sample?.status ?: WaterStatus.UNKNOWN
    val look = status.look
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(look.color)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(look.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(
                        when (status) {
                            WaterStatus.CONFORM -> R.string.hero_conform
                            WaterStatus.RESERVES -> R.string.hero_reserves
                            WaterStatus.NON_CONFORM -> R.string.hero_non_conform
                            WaterStatus.UNKNOWN -> R.string.hero_unknown
                        },
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (sample != null) {
                    Text(stringResource(R.string.hero_sampled_on, Format.longDate(sample.sampledAt)), color = Color.White, fontSize = 13.sp)
                }
            }
        }
        val conclusion = sample?.conclusion ?: if (sample == null) stringResource(R.string.hero_no_sample) else null
        if (conclusion != null) {
            Text(conclusion, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Clock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.hero_monthly), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConformityGrid(sample: WaterSample) {
    val tiles = listOf(
        R.string.flag_bact_limits to sample.flags.bacteriologyLimits,
        R.string.flag_pc_limits to sample.flags.physicoChemicalLimits,
        R.string.flag_bact_references to sample.flags.bacteriologyReferences,
        R.string.flag_pc_references to sample.flags.physicoChemicalReferences,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
    ) {
        tiles.forEach { (label, flag) ->
            AppCard(Modifier.weight(1f), contentPadding = PaddingValues(12.dp)) {
                Text(stringResource(label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Palette.InkSecondary)
                Spacer(Modifier.height(6.dp))
                val (icon, color, text) = when (flag) {
                    Conformity.CONFORM -> Triple(AppIcons.Check, Palette.Conform, R.string.status_conform)
                    Conformity.NON_CONFORM -> Triple(AppIcons.Close, Palette.NonConform, R.string.status_non_conform)
                    Conformity.NOT_ANALYSED -> Triple(AppIcons.Minus, Palette.InkSecondary, R.string.status_unknown)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(text), color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun KeyParameters(sample: WaterSample, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.commune_key_parameters)) {
            TextButton(onClick = onSeeAll) {
                Text(stringResource(R.string.action_see_all), color = Palette.Water, fontWeight = FontWeight.SemiBold)
            }
        }
        val parameters = sample.keyParameters()
        AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            if (parameters.isEmpty()) {
                Text(
                    stringResource(R.string.commune_no_key_parameter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.InkSecondary,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            parameters.forEachIndexed { index, result ->
                if (index > 0) HorizontalDivider(color = Palette.Divider)
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(result.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(result.displayValue, style = MaterialTheme.typography.titleSmall.copy(fontFamily = Sora))
                        result.unitLabel?.let {
                            Spacer(Modifier.width(4.dp))
                            Text(it, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Palette.InkSecondary)
                        }
                    }
                    Gauge(result.fillRatio ?: 0f, result.gaugeColor())
                    Text(boundLabel(result), fontSize = 12.sp, color = Palette.InkSecondary)
                }
            }
        }
    }
}

@Composable
private fun EnvironmentTile(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBackground), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, fontSize = 12.sp, color = Palette.InkSecondary)
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun DroughtTile(state: LoadState<DroughtStatus>, onClick: () -> Unit) {
    val status = state.valueOrNull
    val level = status?.level ?: DroughtLevel.NONE
    val subtitle = when {
        state is LoadState.Loading -> stringResource(R.string.loading)
        state is LoadState.Failed -> stringResource(R.string.error_short)
        level == DroughtLevel.NONE -> stringResource(R.string.drought_none_short)
        status?.validUntil != null -> stringResource(droughtAdvice(level), Format.mediumDate(status.validUntil!!))
        else -> stringResource(droughtAdviceNoDate(level))
    }
    EnvironmentTile(AppIcons.Drought, Palette.DroughtIcon, Palette.DroughtBg, stringResource(R.string.drought_title), subtitle, onClick) {
        if (status != null) DroughtBadge(level) else ChevronIcon()
    }
}

@Composable
private fun RisksTile(state: LoadState<CommuneRisks>, onClick: () -> Unit) {
    val risks = state.valueOrNull
    val subtitle = when {
        state is LoadState.Loading -> stringResource(R.string.loading)
        state is LoadState.Failed -> stringResource(R.string.error_short)
        risks == null || risks.groups.isEmpty() -> stringResource(R.string.risks_none_short)
        else -> {
            val names = risks.groups.take(3).joinToString(", ") { it.name.lowercase().replaceFirstChar(Char::titlecase) }
            if (risks.sevesoCount > 0) stringResource(R.string.risks_summary_seveso, names, risks.sevesoCount) else names
        }
    }
    EnvironmentTile(AppIcons.Warning, Palette.Water, Palette.WaterTint, stringResource(R.string.risks_title), subtitle, onClick) {
        ChevronIcon()
    }
}

@Composable
private fun ChevronIcon() {
    Icon(AppIcons.ChevronRight, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(18.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutGrid(onHistory: () -> Unit, onNearby: () -> Unit, onCompare: () -> Unit, onExport: () -> Unit, exportEnabled: Boolean) {
    data class Shortcut(val icon: ImageVector, val title: Int, val body: Int, val enabled: Boolean, val action: () -> Unit)
    val items = listOf(
        Shortcut(AppIcons.Trend, R.string.shortcut_history, R.string.shortcut_history_body, true, onHistory),
        Shortcut(AppIcons.Waves, R.string.shortcut_nearby, R.string.shortcut_nearby_body, true, onNearby),
        Shortcut(AppIcons.Compare, R.string.shortcut_compare, R.string.shortcut_compare_body, true, onCompare),
        Shortcut(AppIcons.Download, R.string.shortcut_export, R.string.shortcut_export_body, exportEnabled, onExport),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
    ) {
        items.forEach { item ->
            AppCard(Modifier.weight(1f).heightIn(min = 112.dp), onClick = if (item.enabled) item.action else null) {
                Icon(item.icon, contentDescription = null, tint = if (item.enabled) Palette.Water else Palette.Muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(item.title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(item.body), fontSize = 12.sp, color = Palette.InkSecondary)
            }
        }
    }
}

@Composable
private fun NetworkCard(sample: WaterSample) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Text(stringResource(R.string.network_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        val rows = listOfNotNull(
            sample.networks.takeIf { it.isNotEmpty() }?.let { networks ->
                R.string.network_udi to networks.joinToString("\n") { "${it.name.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar(Char::titlecase) }} · ${it.code}" }
            },
            sample.distributor?.let { R.string.network_distributor to it },
            sample.owner?.let { R.string.network_owner to it },
            R.string.network_sample to sample.code,
        )
        rows.forEach { (label, value) ->
            Row(Modifier.padding(vertical = 5.dp)) {
                Text(stringResource(label), fontSize = 13.sp, color = Palette.InkSecondary, modifier = Modifier.weight(1f))
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }
    }
}
