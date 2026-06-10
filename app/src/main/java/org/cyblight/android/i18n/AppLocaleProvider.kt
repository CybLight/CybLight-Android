package org.cyblight.android.i18n

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppLocaleProvider(
    localeTag: String,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    val localizedContext = remember(localeTag, baseContext) {
        LocaleManager.wrapContext(baseContext, localeTag)
    }
    val localizedConfiguration = remember(localeTag, baseConfiguration) {
        Configuration(baseConfiguration).apply {
            setLocale(LocaleManager.toLocale(localeTag))
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
    ) {
        content()
    }
}
