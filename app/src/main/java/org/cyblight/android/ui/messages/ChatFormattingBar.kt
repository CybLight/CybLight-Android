package org.cyblight.android.ui.messages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.cyblight.android.R

@Composable
fun ChatFormattingBar(
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
    onShowLinkDialog: () -> Unit,
    modifier: Modifier = Modifier,
    includeClipboard: Boolean = false,
    onCut: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onPaste: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (includeClipboard) {
            FormatIconButton(
                icon = Icons.Outlined.ContentCut,
                contentDescription = stringResource(R.string.chat_format_cut),
                onClick = { onCut?.invoke() },
            )
            FormatIconButton(
                icon = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.chat_action_copy),
                onClick = { onCopy?.invoke() },
            )
            FormatIconButton(
                icon = Icons.Outlined.ContentPaste,
                contentDescription = stringResource(R.string.chat_format_paste),
                onClick = { onPaste?.invoke() },
            )
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
        }

        FormatIconButton(
            icon = Icons.Outlined.FormatQuote,
            contentDescription = stringResource(R.string.chat_format_quote),
            onClick = { ChatFormatActions.insertBlockquote(draft, onDraftChange) },
        )
        FormatIconButton(
            icon = Icons.Outlined.BlurOn,
            contentDescription = stringResource(R.string.chat_format_spoiler),
            onClick = { ChatFormatActions.wrapSelection(draft, onDraftChange, "||", "||") },
        )
        FormatIconButton(
            icon = Icons.Outlined.FormatBold,
            contentDescription = stringResource(R.string.chat_format_bold),
            onClick = { ChatFormatActions.wrapSelection(draft, onDraftChange, "**", "**") },
        )
        FormatIconButton(
            icon = Icons.Outlined.FormatItalic,
            contentDescription = stringResource(R.string.chat_format_italic),
            onClick = { ChatFormatActions.wrapSelection(draft, onDraftChange, "_", "_") },
        )
        FormatIconButton(
            icon = Icons.Outlined.Code,
            contentDescription = stringResource(R.string.chat_format_mono),
            onClick = { ChatFormatActions.wrapSelection(draft, onDraftChange, "`", "`") },
        )
        FormatIconButton(
            icon = Icons.Outlined.FormatStrikethrough,
            contentDescription = stringResource(R.string.chat_format_strike),
            onClick = { ChatFormatActions.wrapSelection(draft, onDraftChange, "~~", "~~") },
        )
        FormatIconButton(
            icon = Icons.Outlined.FormatUnderlined,
            contentDescription = stringResource(R.string.chat_format_underline),
            onClick = { ChatFormatActions.wrapSelection(draft, onDraftChange, "__", "__") },
        )
        FormatIconButton(
            icon = Icons.Outlined.Link,
            contentDescription = stringResource(R.string.chat_format_link),
            onClick = onShowLinkDialog,
        )
        FormatIconButton(
            icon = Icons.Outlined.DataObject,
            contentDescription = stringResource(R.string.chat_format_code),
            onClick = {
                val selected = draft.text.substring(draft.selection.min, draft.selection.max)
                val block = "```\n${selected.ifBlank { "code" }}\n```"
                ChatFormatActions.insertAtSelection(draft, onDraftChange, block)
            },
        )
        FormatIconButton(
            icon = Icons.Outlined.FormatClear,
            contentDescription = stringResource(R.string.chat_format_regular),
            onClick = { ChatFormatActions.stripFormatting(draft, onDraftChange) },
        )
    }
}

@Composable
private fun FormatIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
