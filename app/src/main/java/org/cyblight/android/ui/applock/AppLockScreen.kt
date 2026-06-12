package org.cyblight.android.ui.applock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.cyblight.android.R
import org.cyblight.android.ui.components.CybLightLogo

private const val MAX_BIOMETRIC_FAILURES = 3

private enum class AppLockMode {
    Biometric,
    Pin,
}

@Composable
fun AppLockScreen(
    sessionId: Int,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    errorMessage: String?,
    onUnlockPin: (String) -> Unit,
    onUnlockBiometric: (
        onSuccess: () -> Unit,
        onFailed: () -> Unit,
        onError: (Int) -> Unit,
    ) -> Unit,
) {
    val useBiometricFirst = biometricAvailable && biometricEnabled

    key(sessionId) {
        var mode by remember { mutableStateOf(if (useBiometricFirst) AppLockMode.Biometric else AppLockMode.Pin) }
        var pin by remember { mutableStateOf("") }
        var biometricFailures by remember { mutableIntStateOf(0) }
        var biometricPromptActive by remember { mutableStateOf(false) }

        fun showPinFallback() {
            mode = AppLockMode.Pin
            biometricPromptActive = false
        }

        fun triggerBiometric() {
            if (biometricPromptActive || mode != AppLockMode.Biometric) return
            biometricPromptActive = true
            onUnlockBiometric(
                {
                    biometricPromptActive = false
                },
                {
                    biometricFailures++
                    biometricPromptActive = false
                    if (biometricFailures >= MAX_BIOMETRIC_FAILURES) {
                        showPinFallback()
                    }
                },
                { errorCode ->
                    biometricPromptActive = false
                    if (errorCode == androidx.biometric.BiometricPrompt.ERROR_LOCKOUT ||
                        errorCode == androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
                    ) {
                        showPinFallback()
                    }
                },
            )
        }

        LaunchedEffect(useBiometricFirst, mode) {
            if (useBiometricFirst && mode == AppLockMode.Biometric) {
                delay(150)
                triggerBiometric()
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CybLightLogo(modifier = Modifier.padding(bottom = 24.dp))
                Icon(
                    if (mode == AppLockMode.Biometric) Icons.Outlined.Fingerprint else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = stringResource(R.string.app_lock_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (mode == AppLockMode.Biometric) {
                        stringResource(R.string.app_lock_biometric_waiting)
                    } else {
                        stringResource(R.string.app_lock_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )

                if (mode == AppLockMode.Biometric) {
                    if (biometricPromptActive) {
                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                    }

                    OutlinedButton(
                        onClick = ::triggerBiometric,
                        enabled = !biometricPromptActive,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                        Text(
                            text = stringResource(R.string.app_lock_biometric_retry),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    if (biometricFailures > 0) {
                        Text(
                            text = stringResource(
                                R.string.app_lock_biometric_failures,
                                biometricFailures,
                                MAX_BIOMETRIC_FAILURES,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { value ->
                            if (value.length <= 8 && value.all { it.isDigit() }) {
                                pin = value
                            }
                        },
                        label = { Text(stringResource(R.string.app_lock_pin_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Button(
                        onClick = {
                            onUnlockPin(pin)
                            pin = ""
                        },
                        enabled = pin.length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(stringResource(R.string.app_lock_unlock))
                    }

                    if (useBiometricFirst) {
                        OutlinedButton(
                            onClick = {
                                mode = AppLockMode.Biometric
                                biometricFailures = 0
                                triggerBiometric()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                            Text(
                                text = stringResource(R.string.app_lock_biometric),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val pinTooShort = stringResource(R.string.app_lock_pin_too_short)
    val pinMismatch = stringResource(R.string.app_lock_pin_mismatch)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        if (value.length <= 8 && value.all { it.isDigit() }) pin = value
                    },
                    label = { Text(stringResource(R.string.app_lock_pin_new)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { value ->
                        if (value.length <= 8 && value.all { it.isDigit() }) confirmPin = value
                    },
                    label = { Text(stringResource(R.string.app_lock_pin_confirm)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    when {
                        pin.length < 4 -> error = pinTooShort
                        pin != confirmPin -> error = pinMismatch
                        else -> onConfirm(pin)
                    }
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
