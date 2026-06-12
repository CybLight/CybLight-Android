package org.cyblight.android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.ui.components.AppMenu
import org.cyblight.android.ui.components.CybLightLogo
import org.cyblight.android.ui.components.CybAutofillType
import org.cyblight.android.ui.components.CybOutlinedTextField
import org.cyblight.android.ui.components.TurnstileWebView

@Composable
fun LoginScreen(
    isSubmitting: Boolean,
    errorCode: String?,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onCheckUpdates: () -> Unit,
    onReportBug: () -> Unit,
    onDonate: () -> Unit,
    onLogin: (login: String, password: String, turnstileToken: String) -> Unit,
    onPasskeyLogin: (login: String?) -> Unit,
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var turnstileToken by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMenu(
                onSettings = onSettings,
                onHelp = onHelp,
                onAbout = onAbout,
                onCheckUpdates = onCheckUpdates,
                onReportBug = onReportBug,
                onDonate = onDonate,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        CybLightLogo(size = 112.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.turnstile_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TurnstileWebView(onToken = { turnstileToken = it })
        Spacer(modifier = Modifier.height(16.dp))

        CybOutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = stringResource(R.string.username),
            autofillType = CybAutofillType.Username,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        CybOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            showPasswordToggle = true,
            autofillType = CybAutofillType.Password,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!errorCode.isNullOrBlank()) {
            Text(
                text = mapLoginError(errorCode),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { onLogin(login, password, turnstileToken) },
            enabled = !isSubmitting && login.isNotBlank() && password.isNotBlank() && turnstileToken.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.sign_in))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { onPasskeyLogin(login.ifBlank { null }) },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Key, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sign_in_passkey))
            }
        }
    }
}

@Composable
fun TwoFactorScreen(
    isSubmitting: Boolean,
    errorCode: String?,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onCheckUpdates: () -> Unit,
    onReportBug: () -> Unit,
    onDonate: () -> Unit,
    onVerify: (code: String, rememberDevice: Boolean) -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var rememberDevice by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMenu(
                onSettings = onSettings,
                onHelp = onHelp,
                onAbout = onAbout,
                onCheckUpdates = onCheckUpdates,
                onReportBug = onReportBug,
                onDonate = onDonate,
            )
        }
        CybLightLogo()
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.two_factor_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.two_factor_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        CybOutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = stringResource(R.string.two_factor_code),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(
                checked = rememberDevice,
                onCheckedChange = { rememberDevice = it },
            )
            Text(
                text = stringResource(R.string.remember_device),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (!errorCode.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(mapLoginError(errorCode), color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onVerify(code, rememberDevice) },
            enabled = !isSubmitting && code.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isSubmitting) stringResource(R.string.signing_in) else stringResource(R.string.verify))
        }
    }
}

@Composable
private fun mapLoginError(code: String): String = when (code) {
    "invalid_credentials", "missing_fields" -> stringResource(R.string.error_login)
    "invalid_code" -> stringResource(R.string.error_2fa_code)
    "turnstile_required", "turnstile_failed" -> stringResource(R.string.error_turnstile)
    "too_many_requests" -> stringResource(R.string.error_rate_limit)
    "cancelled" -> stringResource(R.string.error_passkey_cancelled)
    "no_passkey" -> stringResource(R.string.error_passkey_not_found)
    "asset_links_failed" -> stringResource(R.string.error_passkey_asset_links_failed)
    "passkey_failed", "missing_token", "invalid_response",
    "origin_mismatch", "invalid_challenge", "challenge_mismatch",
    -> stringResource(R.string.error_passkey_failed)
    else -> stringResource(R.string.error_generic)
}
