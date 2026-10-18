package fr.mandarine.environmentquality.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import fr.mandarine.environmentquality.ui.theme.EnvironmentQualityTheme

/** The single activity. It calls setContent and owns no views. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnvironmentQualityTheme {
                EnvironmentQualityRoot()
            }
        }
    }
}
