package fr.mandarine.environmentquality.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.environmentquality.AppContainer
import fr.mandarine.environmentquality.EnvironmentQualityApp
import fr.mandarine.environmentquality.data.local.AppDatabase
import fr.mandarine.environmentquality.data.remote.FakeHttpClient
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.ui.navigation.MapKey
import fr.mandarine.environmentquality.ui.navigation.Navigator
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
import fr.mandarine.environmentquality.ui.theme.EnvironmentQualityTheme
import fr.mandarine.environmentquality.ui.theme.Palette
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every screen against the recorded API answers in `fixtures/`, checks
 * what a user would look for, and saves a screenshot of each under
 * `app/build/qa-screenshots/` for a visual review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "fr-w390dp-h1400dp-mdpi")
class ScreensQaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val app: EnvironmentQualityApp = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private val navigator = Navigator(mutableStateListOf<NavKey>(MapKey))
    private val vernon = Commune("27681", "Vernon", departmentCode = "27")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        app.container = AppContainer(app, FakeHttpClient(ROUTES), database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should show the water layer and reference cities when the map opens`() {
        show("map") { MapScreen(navigator) }
        waitFor("Eau potable")
    }

    @Test
    fun `should show the search field when the search tab opens`() {
        show("search") { SearchScreen(navigator) }
        waitFor("Commune ou code postal")
    }

    @Test
    fun `should show the latest sample and key parameters when a commune opens from an alert`() {
        composeRule.setContent { EnvironmentQualityTheme { EnvironmentQualityRoot(alertCommune = "27681") } }
        waitFor("Paramètres clés")
        waitFor("Vernon")
        capture("commune")
    }

    @Test
    fun `should list the analysed parameters when the parameters screen opens`() {
        show("parameters") { ParametersScreen("27681", navigator) }
        waitFor("Nitrates")
    }

    @Test
    fun `should show the monthly compliance when the history screen opens`() {
        show("history") { HistoryScreen("27681", navigator) }
        waitFor("Conformité mois par mois")
    }

    @Test
    fun `should show the restriction level and uses when the drought screen opens`() {
        show("drought") { DroughtScreen("27681", navigator) }
        waitFor("Ce qui est restreint")
        waitFor("Lire l'arrêté")
    }

    @Test
    fun `should show seismic zone, risk families and sites when the risks screen opens`() {
        show("risks") { RisksScreen("27681", navigator) }
        waitFor("risques recensés")
        waitFor("Sites industriels")
    }

    @Test
    fun `should show river, temperature, flows and groundwater when the nearby screen opens`() {
        show("nearby") { NearbyScreen("27681", navigator) }
        waitFor("Rivière en direct")
        waitFor("Nappe la plus proche")
        waitFor("Température de l'eau")
    }

    @Test
    fun `should hide the river sections when the groundwater tab is picked`() {
        show("nearby-groundwater") { NearbyScreen("27681", navigator) }
        waitFor("Rivière en direct")
        composeRule.onNodeWithText("Nappes").performClick()
        composeRule.waitForIdle()
        assertFalse(exists("Rivière en direct"))
        assertTrue(exists("Nappe la plus proche"))
        capture("nearby-groundwater")
    }

    @Test
    fun `should put both communes side by side when the compare screen opens`() {
        show("compare") { CompareScreen("27681", navigator) }
        waitFor("Mois conformes")
    }

    @Test
    fun `should list followed communes and the alert switches when the follows tab opens`() {
        runBlocking { app.container.followCommune.follow(vernon) }
        show("follows") { FollowsScreen(navigator) }
        waitFor("Vernon")
        waitFor("Référence dépassée")
        waitFor("Comparer mes communes")
    }

    @Test
    fun `should explain statuses and sources when the sources tab opens`() {
        show("sources") { SourcesScreen(navigator) }
        waitFor("Lire les statuts")
        waitFor("Résultats jusqu'au")
    }

    @Test
    @Config(qualifiers = "en-w390dp-h1400dp-mdpi")
    fun `should show the sources tab in English when the device is in English`() {
        show("sources-en") { SourcesScreen(navigator) }
        waitFor("Reading the statuses")
        waitFor("Results up to")
    }

    private fun show(name: String, content: @Composable () -> Unit) {
        composeRule.setContent {
            EnvironmentQualityTheme {
                Box(Modifier.fillMaxSize().background(Palette.Background)) { content() }
            }
        }
        // Screenshots are taken once the screen has loaded; see waitFor.
        pendingCapture = name
    }

    private var pendingCapture: String? = null

    private fun exists(text: String): Boolean =
        composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) { exists(text) }
        pendingCapture?.let { capture(it) }
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/qa-screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
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
