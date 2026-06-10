package org.cyblight.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.cyblight.android.BuildConfig

object BugReport {
    fun open(context: Context) {
        val body = buildString {
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("Describe the issue:")
        }
        val uri = Uri.parse(BuildConfig.BUG_REPORT_URL)
            .buildUpon()
            .appendQueryParameter("body", body)
            .build()

        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
