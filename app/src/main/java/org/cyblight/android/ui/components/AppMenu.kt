package org.cyblight.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.cyblight.android.R

@Composable
fun AppMenu(
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onCheckUpdates: () -> Unit,
    onReportBug: () -> Unit,
    onDonate: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Outlined.Apps,
            contentDescription = stringResource(R.string.menu_app),
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_settings)) },
            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            onClick = {
                expanded = false
                onSettings()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_help)) },
            leadingIcon = { Icon(Icons.Outlined.HelpOutline, contentDescription = null) },
            onClick = {
                expanded = false
                onHelp()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_about)) },
            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            onClick = {
                expanded = false
                onAbout()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_check_updates)) },
            leadingIcon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
            onClick = {
                expanded = false
                onCheckUpdates()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_report_bug)) },
            leadingIcon = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
            onClick = {
                expanded = false
                onReportBug()
            },
        )
        if (onDonate != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_donate)) },
                leadingIcon = { Icon(Icons.Outlined.Favorite, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDonate()
                },
            )
        }
        if (onLogout != null) {
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.logout),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    expanded = false
                    onLogout()
                },
            )
        }
    }
}
