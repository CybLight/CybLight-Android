package org.cyblight.android.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object SystemSettings {
    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openAppNotificationSettings(context: Context) {
        val packageName = context.packageName
        val intents = listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra("android.provider.extra.APP_PACKAGE", packageName)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            },
        )
        for (intent in intents) {
            if (launchSettings(context, intent)) return
        }
    }

    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (isIgnoringBatteryOptimizations(activity)) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        runCatching { activity.startActivity(intent) }
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        launchSettings(context, intent)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        launchSettings(context, intent)
    }

    private fun launchSettings(context: Context, intent: Intent): Boolean {
        val activity = ExternalLinks.findActivity(context)
        val launchIntent = intent.apply {
            if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            (activity ?: context).startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }
}
