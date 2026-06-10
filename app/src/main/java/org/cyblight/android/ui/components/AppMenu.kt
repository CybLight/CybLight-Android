package org.cyblight.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SystemUpdate
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

@Composable
fun AppMenu(
    onAbout: () -> Unit,
    onCheckUpdates: () -> Unit,
    onReportBug: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.menu_app),
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
    }
}
