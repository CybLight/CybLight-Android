package org.cyblight.android

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.i18n.LocaleManager

class CybLightApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val locale = runBlocking { SessionManager(applicationContext).getLocale() }
        LocaleManager.apply(locale)
    }
}
