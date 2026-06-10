@file:OptIn(ExperimentalComposeUiApi::class)

package org.cyblight.android.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import org.cyblight.android.R

enum class CybAutofillType {
    Username,
    Password,
}

private fun CybAutofillType.toAutofillType(): AutofillType = when (this) {
    CybAutofillType.Username -> AutofillType.Username
    CybAutofillType.Password -> AutofillType.Password
}

@Composable
fun CybOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    showPasswordToggle: Boolean = false,
    autofillType: CybAutofillType? = null,
) {
    val autofillTypes = autofillType?.let { listOf(it.toAutofillType()) } ?: emptyList()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val resolvedTransformation = when {
        showPasswordToggle && !passwordVisible -> PasswordVisualTransformation()
        else -> visualTransformation
    }

    val autofillNode = if (autofillTypes.isNotEmpty()) {
        remember(autofillTypes, onValueChange) {
            AutofillNode(
                autofillTypes = autofillTypes,
                onFill = onValueChange,
            )
        }
    } else {
        null
    }
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current

    if (autofillNode != null) {
        DisposableEffect(autofillNode, autofillTree) {
            autofillTree += autofillNode
            onDispose {
                @Suppress("UNCHECKED_CAST")
                (autofillTree.children as MutableCollection<AutofillNode>).remove(autofillNode)
            }
        }
    }

    val autofillModifier = if (autofillNode != null) {
        Modifier
            .onGloballyPositioned { coordinates ->
                autofillNode.boundingBox = coordinates.boundsInWindow()
            }
            .onFocusChanged { focusState ->
                if (autofill != null) {
                    if (focusState.isFocused) {
                        autofill.requestAutofillForNode(autofillNode)
                    } else {
                        autofill.cancelAutofillForNode(autofillNode)
                    }
                }
            }
    } else {
        Modifier
    }

    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { labelText -> { Text(labelText) } },
        placeholder = placeholder?.let { placeholderText -> { Text(placeholderText) } },
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = resolvedTransformation,
        keyboardOptions = keyboardOptions,
        modifier = modifier.then(autofillModifier),
        colors = colors,
        trailingIcon = if (showPasswordToggle) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.hide_password else R.string.show_password,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            null
        },
    )
}
