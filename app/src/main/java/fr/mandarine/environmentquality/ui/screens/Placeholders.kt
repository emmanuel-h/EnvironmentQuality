package fr.mandarine.environmentquality.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.ui.components.EmptyText
import fr.mandarine.environmentquality.ui.components.ScreenHeader
import fr.mandarine.environmentquality.ui.navigation.Navigator

/** Screens not built yet show their title; each is replaced as it lands. */
@Composable
private fun Placeholder(title: String, navigator: Navigator, back: Boolean) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, subtitle = null, onBack = if (back) navigator::back else null)
        EmptyText(stringResource(R.string.coming_soon))
    }
}

@Composable fun FollowsScreen(navigator: Navigator) = Placeholder(stringResource(R.string.tab_follows), navigator, back = false)

@Composable fun SourcesScreen(navigator: Navigator) = Placeholder(stringResource(R.string.tab_sources), navigator, back = false)

@Composable fun CommuneScreen(code: String, navigator: Navigator) = Placeholder(code, navigator, back = true)

@Composable fun ParametersScreen(code: String, navigator: Navigator) = Placeholder(code, navigator, back = true)

@Composable fun HistoryScreen(code: String, navigator: Navigator) = Placeholder(code, navigator, back = true)

@Composable fun NearbyScreen(code: String, navigator: Navigator) = Placeholder(code, navigator, back = true)

@Composable fun CompareScreen(code: String, navigator: Navigator) = Placeholder(code, navigator, back = true)

@Composable fun DroughtScreen(code: String, navigator: Navigator) = Placeholder(code, navigator, back = true)

@Composable fun RisksScreen(code: String, navigator: Navigator) = Placeholder(code, navigator, back = true)
