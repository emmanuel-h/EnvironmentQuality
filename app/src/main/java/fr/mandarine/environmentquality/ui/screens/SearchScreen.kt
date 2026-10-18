package fr.mandarine.environmentquality.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.presentation.LoadState
import fr.mandarine.environmentquality.presentation.LocateState
import fr.mandarine.environmentquality.presentation.SearchUiState
import fr.mandarine.environmentquality.presentation.SearchViewModel
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.components.CardShape
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.OverlineTitle
import fr.mandarine.environmentquality.ui.components.StatusMark
import fr.mandarine.environmentquality.ui.navigation.CommuneKey
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.theme.Palette
import fr.mandarine.environmentquality.ui.theme.PublicSans

@Composable
fun SearchScreen(navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel {
        SearchViewModel(container.searchCommunes, container.communes, container.water, container.location, container.recent)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusField = remember { navigator.consumeSearchFocus() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.locate() else viewModel.locationDenied()
    }
    LaunchedEffect(state.locate) {
        val found = state.locate as? LocateState.Found ?: return@LaunchedEffect
        viewModel.locateHandled()
        navigator.open(CommuneKey(found.commune.code))
    }
    SearchContent(
        state = state,
        onQuery = viewModel::onQueryChange,
        onLocate = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.locate() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        onOpen = { navigator.open(CommuneKey(it.code)) },
        focusField = focusField,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchContent(
    state: SearchUiState,
    onQuery: (String) -> Unit,
    onLocate: () -> Unit,
    onOpen: (Commune) -> Unit,
    focusField: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusField) {
        if (focusField) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .background(Palette.Surface)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.tab_search), style = MaterialTheme.typography.headlineMedium)
            val hint = stringResource(R.string.search_hint)
            // The whole field is the text field, so a tap anywhere in it brings up the keyboard.
            BasicTextField(
                value = state.query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(fontFamily = PublicSans, fontSize = 16.sp, color = Palette.Ink),
                cursorBrush = SolidColor(Palette.Water),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = hint },
                decorationBox = { innerField ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.Field)
                            .border(2.dp, Palette.Water, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(AppIcons.Search, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.weight(1f)) {
                            if (state.query.isEmpty()) Text(hint, color = Palette.InkSecondary, fontSize = 16.sp)
                            innerField()
                        }
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQuery("") }) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(Palette.ChipBorder), contentAlignment = Alignment.Center) {
                                    Icon(AppIcons.Close, stringResource(R.string.action_clear), tint = Palette.Ink, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button, onClick = onLocate),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Locate, contentDescription = null, tint = Palette.Water, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(if (state.locate == LocateState.Locating) R.string.search_locating else R.string.search_use_location),
                    color = Palette.Water,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
            if (state.locate == LocateState.Failed) {
                Text(stringResource(R.string.search_location_failed), style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider(color = Palette.Border)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (val results = state.results) {
                    null -> OverlineTitle(stringResource(R.string.search_type_prompt))
                    LoadState.Loading -> {
                        OverlineTitle(stringResource(R.string.search_type_prompt))
                        LoadingBox()
                    }
                    is LoadState.Failed -> Text(stringResource(R.string.error_generic), style = MaterialTheme.typography.bodyMedium)
                    is LoadState.Loaded -> {
                        OverlineTitle(pluralStringResource(R.plurals.search_result_count, results.value.size, results.value.size))
                        Column(
                            Modifier
                                .clip(CardShape)
                                .background(Palette.Surface)
                                .border(1.dp, Palette.Border, CardShape),
                        ) {
                            if (results.value.isEmpty()) {
                                Text(
                                    stringResource(R.string.search_no_result),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Palette.InkSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                                )
                            }
                            results.value.forEachIndexed { index, commune ->
                                if (index > 0) HorizontalDivider(color = Palette.Divider)
                                ResultRow(commune) { onOpen(commune) }
                            }
                        }
                    }
                }
            }
            if (state.recent.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlineTitle(stringResource(R.string.search_recent))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recent.forEach { recent ->
                            Row(
                                Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Palette.Surface)
                                    .border(1.dp, Palette.ChipBorder, RoundedCornerShape(20.dp))
                                    .clickable(role = Role.Button) { onOpen(recent.commune) }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusMark(recent.status, 10.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(recent.commune.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            Text(stringResource(R.string.search_footer), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ResultRow(commune: Commune, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Palette.WaterTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(commune.departmentCode ?: "", color = Palette.Water, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(commune.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            val population = commune.population?.let { stringResource(R.string.population, Format.integer(it)) }
            Text(listOfNotNull(Format.place(commune), population).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        }
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(18.dp))
    }
}
