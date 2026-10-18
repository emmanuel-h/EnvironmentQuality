package fr.mandarine.environmentquality.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import fr.mandarine.environmentquality.ui.theme.EnvironmentQualityTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposeToolchainTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should compose and expose a semantics tree under Robolectric`() {
        composeRule.setContent {
            Box(modifier = Modifier.testTag("probe")) {
                Text("composed")
            }
        }

        composeRule.onNodeWithTag("probe").assertIsDisplayed()
    }

    @Test
    fun `should show the four tabs when the app is composed`() {
        composeRule.setContent { EnvironmentQualityTheme { EnvironmentQualityRoot() } }

        listOf("Carte", "Rechercher", "Suivis", "Sources").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun `should launch the main activity without finishing when started`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
            }
        }
    }
}
