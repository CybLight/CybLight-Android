package org.cyblight.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.cyblight.android.R
import org.cyblight.android.i18n.LocaleManager

@Composable
fun LanguageMenu(
    currentLocale: String,
    onLocaleSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Outlined.Language, contentDescription = stringResource(R.string.language))
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        LocaleManager.supported.forEach { locale ->
            DropdownMenuItem(
                text = { Text(LocaleManager.displayName(locale)) },
                onClick = {
                    expanded = false
                    if (locale != currentLocale) onLocaleSelected(locale)
                },
            )
        }
    }
}
