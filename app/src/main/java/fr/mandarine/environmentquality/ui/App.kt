package fr.mandarine.environmentquality.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import fr.mandarine.environmentquality.AppContainer
import fr.mandarine.environmentquality.EnvironmentQualityApp
import fr.mandarine.environmentquality.ui.navigation.BottomBar
import fr.mandarine.environmentquality.ui.navigation.CommuneKey
import fr.mandarine.environmentquality.ui.navigation.CompareKey
import fr.mandarine.environmentquality.ui.navigation.DroughtKey
import fr.mandarine.environmentquality.ui.navigation.FollowsKey
import fr.mandarine.environmentquality.ui.navigation.HistoryKey
import fr.mandarine.environmentquality.ui.navigation.MapKey
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.navigation.NearbyKey
import fr.mandarine.environmentquality.ui.navigation.ParametersKey
import fr.mandarine.environmentquality.ui.navigation.RisksKey
import fr.mandarine.environmentquality.ui.navigation.SearchKey
import fr.mandarine.environmentquality.ui.navigation.SourcesKey
import fr.mandarine.environmentquality.ui.screens.CommuneScreen
import fr.mandarine.environmentquality.ui.screens.CompareScreen
import fr.mandarine.environmentquality.ui.screens.DroughtScreen
import fr.mandarine.environmentquality.ui.screens.FollowsScreen
import fr.mandarine.environmentquality.ui.screens.HistoryScreen
import fr.mandarine.environmentquality.ui.screens.MapScreen
import fr.mandarine.environmentquality.ui.screens.NearbyScreen
import fr.mandarine.environmentquality.ui.screens.ParametersScreen
import fr.mandarine.environmentquality.ui.screens.RisksScreen
import fr.mandarine.environmentquality.ui.screens.SearchScreen
import fr.mandarine.environmentquality.ui.screens.SourcesScreen
import fr.mandarine.environmentquality.ui.theme.Palette

/** The process-wide container, reached from any composable. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as EnvironmentQualityApp).container

@Composable
fun EnvironmentQualityRoot(alertCommune: String? = null) {
    val backStack = rememberNavBackStack(MapKey)
    val navigator = remember(backStack) { Navigator(backStack) }
    LaunchedEffect(alertCommune) {
        if (alertCommune != null && backStack.lastOrNull() != CommuneKey(alertCommune)) navigator.open(CommuneKey(alertCommune))
    }
    Scaffold(
        containerColor = Palette.Background,
        bottomBar = { BottomBar(current = navigator.currentTab, onSelect = navigator::selectTab) },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            onBack = navigator::back,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<MapKey> { MapScreen(navigator) }
                entry<SearchKey> { SearchScreen(navigator) }
                entry<FollowsKey> { FollowsScreen(navigator) }
                entry<SourcesKey> { SourcesScreen(navigator) }
                entry<CommuneKey> { key -> CommuneScreen(key.code, navigator) }
                entry<ParametersKey> { key -> ParametersScreen(key.code, navigator) }
                entry<HistoryKey> { key -> HistoryScreen(key.code, navigator) }
                entry<NearbyKey> { key -> NearbyScreen(key.code, navigator) }
                entry<CompareKey> { key -> CompareScreen(key.code, navigator) }
                entry<DroughtKey> { key -> DroughtScreen(key.code, navigator) }
                entry<RisksKey> { key -> RisksScreen(key.code, navigator) }
            },
        )
    }
}
