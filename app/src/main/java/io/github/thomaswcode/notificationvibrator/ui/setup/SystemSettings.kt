package io.github.thomaswcode.notificationvibrator.ui.setup

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.thomaswcode.notificationvibrator.R
import io.github.thomaswcode.notificationvibrator.service.PatternNotificationListener

/** Status checks and deep links into Android/One UI settings used by the Setup screen. */
object SystemSettings {
    private const val TEST_CHANNEL_ID = "vibration_test"
    private const val TEST_NOTIFICATION_ID = 1

    fun hasNotificationAccess(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    fun isBatteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun canPostNotifications(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openNotificationAccess(context: Context) {
        val component = ComponentName(context, PatternNotificationListener::class.java)
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        launch(context, detail, fallback = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openAppInfo(context: Context) =
        launch(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))

    @Suppress("BatteryLife") // Needed so Samsung does not stop the listener; the app is sideloaded, not on Play
    fun requestUnrestrictedBattery(context: Context) = launch(
        context,
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
        fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    )

    fun openSoundSettings(context: Context) = launch(context, Intent(Settings.ACTION_SOUND_SETTINGS))

    fun openAppNotificationSettings(context: Context, packageName: String) = launch(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
    )

    /**
     * Posts a notification on a channel that asks for vibration. With global mute set up correctly
     * the phone shows it without buzzing. Returns false if notification permission is missing.
     */
    fun postSystemBuzzTest(context: Context): Boolean {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(TEST_CHANNEL_ID, "Vibration test", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Used by Setup to check that the system's own notification vibration is off"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
        val notification = Notification.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vibration)
            .setContentTitle("Vibration test")
            .setContentText("If the phone buzzed for this, the system's notification vibration is still on.")
            .setAutoCancel(true)
            .setTimeoutAfter(15_000)
            .build()
        manager.notify(TEST_NOTIFICATION_ID, notification)
        return true
    }

    private fun launch(context: Context, intent: Intent, fallback: Intent? = null) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            fallback?.let { runCatching { context.startActivity(it) } }
        }
    }
}
