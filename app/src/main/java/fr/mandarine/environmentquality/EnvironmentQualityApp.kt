package fr.mandarine.environmentquality

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import fr.mandarine.environmentquality.data.device.AlertNotifier
import fr.mandarine.environmentquality.data.device.AlertWorker

class EnvironmentQualityApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        internal set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** WorkManager starts on demand with this factory, which hands workers their dependencies. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
                        when (workerClassName) {
                            AlertWorker::class.java.name ->
                                AlertWorker(appContext, workerParameters, container.follows, container.checkFollowed, AlertNotifier(appContext))
                            else -> null
                        }
                },
            )
            .build()
}
