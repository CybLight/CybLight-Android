package org.cyblight.android.util

import android.os.Build
import org.cyblight.android.BuildConfig

object AppUserAgent {
    fun build(): String =
        "CybLight-Android/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER} ${Build.MODEL})"
}
