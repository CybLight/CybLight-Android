package org.cyblight.android.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleManager {
    val supported = listOf("ru", "uk", "en")

    fun apply(localeTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
    }

    fun toLocale(localeTag: String): Locale = when (localeTag) {
        "uk" -> Locale.forLanguageTag("uk")
        "en" -> Locale.ENGLISH
        else -> Locale.forLanguageTag("ru")
    }

    fun wrapContext(base: Context, localeTag: String): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(toLocale(localeTag))
        return base.createConfigurationContext(config)
    }

    fun displayName(localeTag: String): String = when (localeTag) {
        "uk" -> "Українська"
        "en" -> "English"
        else -> "Русский"
    }
}
