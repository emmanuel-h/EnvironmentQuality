package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.AlertSettings
import fr.mandarine.environmentquality.domain.model.FollowedCommune
import fr.mandarine.environmentquality.presentation.FollowsViewModel
import fr.mandarine.environmentquality.ui.appContainer
import fr.mandarine.environmentquality.ui.components.AppCard
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.components.Format
import fr.mandarine.environmentquality.ui.components.LoadingBox
import fr.mandarine.environmentquality.ui.components.SectionTitle
import fr.mandarine.environmentquality.ui.components.StatusMark
import fr.mandarine.environmentquality.ui.components.TabHeader
import fr.mandarine.environmentquality.ui.components.look
import fr.mandarine.environmentquality.ui.components.rememberNotificationPermissionRequest
import fr.mandarine.environmentquality.ui.navigation.CommuneKey
import fr.mandarine.environmentquality.ui.navigation.CompareKey
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.navigation.SearchKey
import fr.mandarine.environmentquality.ui.theme.Palette

@Composable
fun FollowsScreen(navigator: Navigator) {
    val container = appContainer()
    val viewModel = viewModel { FollowsViewModel(container.follows, container.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestPermission = rememberNotificationPermissionRequest()
    Column(Modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.tab_follows), stringResource(R.string.follows_subtitle))
        val followed = state.followed
        if (followed == null) {
            LoadingBox()
            return@Column
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            FollowedList(
                followed,
                onOpen = { navigator.open(CommuneKey(it.commune.code)) },
                onAdd = { navigator.selectTab(SearchKey) },
            )
            AlertsSection(state.alerts) { alerts ->
                if (alerts.nonConform || alerts.newSample || alerts.referenceExceeded) requestPermission()
                viewModel.setAlerts(alerts)
            }
            if (followed.isNotEmpty()) {
                Surface(
                    onClick = { navigator.open(CompareKey(followed.first().commune.code)) },
                    shape = RoundedCornerShape(16.dp),
                    color = Palette.Surface,
                    border = BorderStroke(1.5.dp, Palette.Water),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Compare, contentDescription = null, tint = Palette.Water, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.follows_compare), color = Palette.Water, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowedList(followed: List<FollowedCommune>, onOpen: (FollowedCommune) -> Unit, onAdd: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        if (followed.isEmpty()) {
            Text(
                stringResource(R.string.follows_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.InkSecondary,
                modifier = Modifier.padding(16.dp),
            )
        }
        followed.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onOpen(item) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusMark(item.lastStatus, 36.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.commune.name, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp))
                    val status = stringResource(item.lastStatus.look.label)
                    Text(
                        item.lastSampledAt?.let { stringResource(R.string.follows_meta, status, Format.longDate(it)) } ?: status,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (item.hasUpdate) {
                    Text(
                        stringResource(R.string.follows_new),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.WaterDark,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.WaterLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(AppIcons.ChevronRight, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(18.dp))
            }
            HorizontalDivider(color = Palette.Divider)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onAdd)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Plus, contentDescription = null, tint = Palette.Water, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.follows_add), color = Palette.Water, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AlertsSection(alerts: AlertSettings, onChange: (AlertSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.follows_alerts))
        AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            AlertSwitch(stringResource(R.string.alert_non_conform), stringResource(R.string.alert_non_conform_body), alerts.nonConform) {
                onChange(alerts.copy(nonConform = it))
            }
            HorizontalDivider(color = Palette.Divider)
            AlertSwitch(stringResource(R.string.alert_new_sample), stringResource(R.string.alert_new_sample_body), alerts.newSample) {
                onChange(alerts.copy(newSample = it))
            }
            HorizontalDivider(color = Palette.Divider)
            AlertSwitch(stringResource(R.string.alert_reference), stringResource(R.string.alert_reference_body), alerts.referenceExceeded) {
                onChange(alerts.copy(referenceExceeded = it))
            }
        }
        Text(stringResource(R.string.follows_alerts_note), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
    }
}

@Composable
private fun AlertSwitch(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Water, uncheckedTrackColor = Palette.Handle, uncheckedBorderColor = Palette.Handle),
        )
    }
}
