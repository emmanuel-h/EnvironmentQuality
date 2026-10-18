package fr.mandarine.environmentquality.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.ui.components.AppIcons
import fr.mandarine.environmentquality.ui.theme.Palette

private data class Tab(val root: NavKey, val icon: ImageVector, val label: Int)

private val tabs = listOf(
    Tab(MapKey, AppIcons.Map, R.string.tab_map),
    Tab(SearchKey, AppIcons.Search, R.string.tab_search),
    Tab(FollowsKey, AppIcons.Star, R.string.tab_follows),
    Tab(SourcesKey, AppIcons.Info, R.string.tab_sources),
)

@Composable
fun BottomBar(current: NavKey, onSelect: (NavKey) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(Palette.Surface).navigationBarsPadding()) {
        HorizontalDivider(color = Palette.Border)
        Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            tabs.forEach { tab ->
                val selected = tab.root == current
                val color = if (selected) Palette.Water else Palette.InkSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab.root) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Palette.WaterLight else Palette.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        stringResource(tab.label),
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
