package fr.mandarine.environmentquality.data.device

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.mandarine.environmentquality.domain.repository.FollowRepository
import fr.mandarine.environmentquality.domain.usecase.CheckFollowedCommunesUseCase
import java.util.concurrent.TimeUnit

/**
 * Checks the followed communes for a new sample. Results are published about
 * once a month, so twice a day is plenty and costs little battery.
 */
class AlertWorker(
    context: Context,
    params: WorkerParameters,
    private val follows: FollowRepository,
    private val check: CheckFollowedCommunesUseCase,
    private val notifier: AlertNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (follows.followedNow().isEmpty()) return Result.success()
        return runCatching { check() }
            .fold(
                onSuccess = { alerts ->
                    notifier.show(alerts)
                    Result.success()
                },
                onFailure = { if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure() },
            )
    }

    companion object {
        private const val WORK_NAME = "water-alerts"
        private const val MAX_ATTEMPTS = 3

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AlertWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
