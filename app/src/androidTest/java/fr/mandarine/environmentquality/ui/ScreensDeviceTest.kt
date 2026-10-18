package fr.mandarine.environmentquality.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.navigation3.runtime.NavKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.environmentquality.AppContainer
import fr.mandarine.environmentquality.EnvironmentQualityApp
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.data.local.AppDatabase
import fr.mandarine.environmentquality.data.remote.FixtureHttpClient
import fr.mandarine.environmentquality.ui.navigation.MapKey
import fr.mandarine.environmentquality.ui.navigation.Navigator
import fr.mandarine.environmentquality.ui.screens.DroughtScreen
import fr.mandarine.environmentquality.ui.screens.HistoryScreen
import fr.mandarine.environmentquality.ui.screens.NearbyScreen
import fr.mandarine.environmentquality.ui.screens.RisksScreen
import fr.mandarine.environmentquality.ui.screens.SourcesScreen
import fr.mandarine.environmentquality.ui.theme.EnvironmentQualityTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the commune screens on a device against the recorded API answers.
 * Texts are looked up by resource so the checks hold in the emulator's language.
 */
@RunWith(AndroidJUnit4::class)
class ScreensDeviceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val app: EnvironmentQualityApp = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private val navigator = Navigator(mutableStateListOf<NavKey>(MapKey))

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        app.container = AppContainer(app, FixtureHttpClient(ROUTES), database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun should_show_the_latest_sample_and_key_parameters_when_a_commune_opens_from_an_alert() {
        composeRule.setContent { EnvironmentQualityTheme { EnvironmentQualityRoot(alertCommune = "27681") } }
        waitFor(R.string.commune_key_parameters)
    }

    @Test
    fun should_show_the_monthly_compliance_when_the_history_screen_opens() {
        show { HistoryScreen("27681", navigator) }
        waitFor(R.string.history_monthly)
    }

    @Test
    fun should_show_the_restricted_uses_when_the_drought_screen_opens() {
        show { DroughtScreen("27681", navigator) }
        waitFor(R.string.drought_uses)
    }

    @Test
    fun should_show_the_industrial_sites_when_the_risks_screen_opens() {
        show { RisksScreen("27681", navigator) }
        waitFor(R.string.risks_sites)
    }

    @Test
    fun should_show_river_temperature_and_groundwater_when_the_nearby_screen_opens() {
        show { NearbyScreen("27681", navigator) }
        waitFor(R.string.nearby_live_river)
        waitFor(R.string.nearby_temperature)
        waitFor(R.string.nearby_groundwater)
    }

    @Test
    fun should_explain_the_statuses_when_the_sources_tab_opens() {
        show { SourcesScreen(navigator) }
        waitFor(R.string.sources_statuses)
    }

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { EnvironmentQualityTheme { content() } }
    }

    private fun waitFor(@StringRes text: Int) {
        val label = app.getString(text)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(label, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        val ROUTES = listOf(
            "/communes/27681" to "geo_commune_27681.json",
            "communes?" to "geo_communes_vernon.json",
            "code_parametre=" to "resultats_dis_history.json",
            "date_min_prelevement=" to "resultats_dis_day.json",
            "resultats_dis" to "resultats_dis_latest.json",
            "vigieau.gouv.fr/api/zones" to "vigieau_zones.json",
            "gaspar/risques" to "georisques_risques.json",
            "gaspar/catnat" to "georisques_catnat.json",
            "radon" to "georisques_radon.json",
            "zonage_sismique" to "georisques_sismique.json",
            "installations_classees" to "georisques_installations.json",
            "observations_tr" to "hydro_observations.json",
            "referentiel/stations" to "hydro_stations.json",
            "temperature/station" to "temperature_stations.json",
            "temperature/chronique" to "temperature_chronique.json",
            "ecoulement/stations" to "ecoulement_stations.json",
            "qualite_nappes/stations" to "nappes_stations.json",
        )
    }
}
