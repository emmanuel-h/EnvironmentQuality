package fr.mandarine.environmentquality.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import fr.mandarine.environmentquality.data.device.AlertNotifier
import fr.mandarine.environmentquality.data.device.AlertWorker
import fr.mandarine.environmentquality.ui.theme.EnvironmentQualityTheme

/** The single activity. It calls setContent and owns no views. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlertWorker.schedule(applicationContext)
        // A tapped alert opens its commune once; a recreated activity keeps its own back stack.
        val alertCommune = if (savedInstanceState == null) intent?.getStringExtra(AlertNotifier.EXTRA_COMMUNE_CODE) else null
        setContent {
            EnvironmentQualityTheme {
                EnvironmentQualityRoot(alertCommune)
            }
        }
    }
}
