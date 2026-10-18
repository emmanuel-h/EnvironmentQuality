package fr.mandarine.environmentquality.data.device

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.WaterAlert
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Posts one notification per alert; tapping it opens the commune's page. */
class AlertNotifier(private val context: Context) {

    fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.notification_channel))
            .setDescription(context.getString(R.string.notification_channel_body))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun show(alerts: List<WaterAlert>) {
        if (alerts.isEmpty() || !canNotify()) return
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        alerts.forEach { alert ->
            val code = alert.commune.code
            val title = context.getString(
                when (alert.kind) {
                    WaterAlert.Kind.NON_CONFORM -> R.string.notification_non_conform
                    WaterAlert.Kind.REFERENCE_EXCEEDED -> R.string.notification_reference
                    WaterAlert.Kind.NEW_SAMPLE -> R.string.notification_new_sample
                },
                alert.commune.name,
            )
            val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault())
                .format(alert.sample.sampledAt.atZone(PARIS))
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.water))
                .setContentTitle(title)
                .setContentText(context.getString(R.string.notification_body, date))
                .setContentIntent(openCommune(code))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()
            manager.notify(code.hashCode(), notification)
        }
    }

    private fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** The launcher intent, so the data layer never names the activity class. */
    private fun openCommune(code: String): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_COMMUNE_CODE, code)
        } ?: return null
        return PendingIntent.getActivity(context, code.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CHANNEL_ID = "water_alerts"
        const val EXTRA_COMMUNE_CODE = "commune_code"
        private val PARIS = ZoneId.of("Europe/Paris")
    }
}
