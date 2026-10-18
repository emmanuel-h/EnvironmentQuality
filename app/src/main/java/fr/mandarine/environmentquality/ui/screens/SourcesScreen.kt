package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.data.device.AppLanguage
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.presentation.SourcesViewModel
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.SectionTitle
import fr.mandarine.environmentquality.ui.components.StatusMark
import fr.mandarine.environmentquality.ui.components.TabHeader
import fr.mandarine.environmentquality.ui.components.look
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.theme.Palette

private data class Source(val title: Int, val body: Int, val url: String)

private val Sources = listOf(
    Source(R.string.source_drinking, R.string.source_drinking_body, "https://hubeau.eaufrance.fr/page/api-qualite-eau-potable"),
    Source(R.string.source_hubeau, R.string.source_hubeau_body, "https://hubeau.eaufrance.fr"),
    Source(R.string.source_vigieau, R.string.source_vigieau_body, "https://vigieau.gouv.fr"),
    Source(R.string.source_georisques, R.string.source_georisques_body, "https://www.georisques.gouv.fr"),
    Source(R.string.source_geo, R.string.source_geo_body, "https://geo.api.gouv.fr"),
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun SourcesScreen(navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel { SourcesViewModel(container.water) }
    val latest by viewModel.latestPublication.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.tab_sources), stringResource(R.string.sources_subtitle))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.Ink)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Clock, contentDescription = null, tint = Palette.River, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    val date = latest.valueOrNull
                    Text(
                        if (date != null) stringResource(R.string.sources_latest, Format.longDate(date)) else stringResource(R.string.sources_latest_unknown),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(stringResource(R.string.sources_latest_body), color = Color(0xFFC9D6DB), fontSize = 13.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.sources_statuses))
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    listOf(
                        WaterStatus.CONFORM to R.string.sources_conform_body,
                        WaterStatus.RESERVES to R.string.sources_reserves_body,
                        WaterStatus.NON_CONFORM to R.string.sources_non_conform_body,
                    ).forEachIndexed { index, (status, body) ->
                        if (index > 0) HorizontalDivider(color = Palette.Divider)
                        Row(Modifier.padding(vertical = 12.dp)) {
                            StatusMark(status, 28.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(status.look.label), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(body), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.sources_open_data))
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Sources.forEachIndexed { index, source ->
                        if (index > 0) HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { runCatching { uriHandler.openUri(source.url) } }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(source.title), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(source.body), style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.width(10.dp))
                            Icon(AppIcons.External, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            LanguageRow()

            Text(stringResource(R.string.sources_disclaimer), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
        }
    }
}

@Composable
private fun LanguageRow() {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val current = AppLanguage.current(context)
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sources_language), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .width(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Field)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Each language is named in itself, as language pickers usually are.
                listOf(AppLanguage.FRENCH to "Français", AppLanguage.ENGLISH to "English").forEach { (tag, name) ->
                    val selected = tag == current
                    Text(
                        tag.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (selected) Palette.Ink else Palette.InkSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) Palette.Surface else Color.Transparent)
                            .selectable(selected = selected, role = Role.RadioButton) {
                                if (!selected && activity != null) AppLanguage.set(activity, tag)
                            }
                            .semantics { contentDescription = name }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}
