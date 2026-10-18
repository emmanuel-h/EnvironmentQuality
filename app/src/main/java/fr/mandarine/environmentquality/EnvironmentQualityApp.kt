package fr.mandarine.environmentquality

import android.app.Application

class EnvironmentQualityApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
