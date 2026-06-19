package org.cyblight.android.ui.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.cyblight.android.R

private val FormatMenuItemHeight = 48.dp
private const val FormatMenuVisibleItemCount = 5

@Composable
fun ChatTextSelectionBar(
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
    onShowLinkDialog: () -> Unit,
    onFormatFromMenu: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!ChatFormatActions.hasSelection(draft)) return

    val clipboard = LocalClipboardManager.current
    var showFormatMenu by remember { mutableStateOf(false) }

    LaunchedEffect(draft.selection.min, draft.selection.max) {
        showFormatMenu = false
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AnimatedContent(
            targetState = showFormatMenu,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 3 } + fadeOut()) using SizeTransform(clip = false)
                } else {
                    (slideInVertically { -it / 3 } + fadeIn()) togetherWith
                        (slideOutVertically { it / 2 } + fadeOut()) using SizeTransform(clip = false)
                }
            },
            label = "chat-selection-menu",
        ) { formatMenuVisible ->
            if (formatMenuVisible) {
                FormatSelectionMenu(
                    draft = draft,
                    onDraftChange = onDraftChange,
                    onShowLinkDialog = onShowLinkDialog,
                    onFormatFromMenu = onFormatFromMenu,
                    onBack = { showFormatMenu = false },
                )
            } else {
                PrimarySelectionMenu(
                    onCopy = {
                        ChatFormatActions.copy(draft) { clipboard.setText(AnnotatedString(it)) }
                    },
                    onPaste = {
                        ChatFormatActions.paste(draft, onDraftChange, clipboard.getText()?.text)
                    },
                    onCut = {
                        ChatFormatActions.cut(draft, onDraftChange) { clipboard.setText(AnnotatedString(it)) }
                    },
                    onMore = { showFormatMenu = true },
                )
            }
        }
    }
}

@Composable
private fun PrimarySelectionMenu(
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onCut: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SelectionTextActionButton(
            text = stringResource(R.string.chat_action_copy),
            onClick = onCopy,
        )
        SelectionTextActionButton(
            text = stringResource(R.string.chat_format_paste),
            onClick = onPaste,
        )
        SelectionTextActionButton(
            text = stringResource(R.string.chat_format_cut),
            onClick = onCut,
        )
        IconButton(onClick = onMore) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.chat_format_more),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FormatSelectionMenu(
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
    onShowLinkDialog: () -> Unit,
    onFormatFromMenu: () -> Unit,
    onBack: () -> Unit,
) {
    fun formatAction(action: () -> Unit) {
        onFormatFromMenu()
        action()
    }
    Column(
        modifier = Modifier
            .widthIn(min = 220.dp, max = 280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f))
            .padding(vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = FormatMenuItemHeight * FormatMenuVisibleItemCount)
                .verticalScroll(rememberScrollState()),
        ) {
            FormatMenuItem(
                icon = Icons.Outlined.FormatQuote,
                text = stringResource(R.string.chat_format_quote),
                onClick = { formatAction { ChatFormatActions.insertBlockquote(draft, onDraftChange) } },
            )
            FormatMenuItem(
                icon = Icons.Outlined.BlurOn,
                text = stringResource(R.string.chat_format_spoiler),
                onClick = { formatAction { ChatFormatActions.wrapSelection(draft, onDraftChange, "||", "||") } },
            )
            FormatMenuItem(
                icon = Icons.Outlined.FormatBold,
                text = stringResource(R.string.chat_format_bold),
                onClick = { formatAction { ChatFormatActions.wrapSelection(draft, onDraftChange, "**", "**") } },
            )
            FormatMenuItem(
                icon = Icons.Outlined.FormatItalic,
                text = stringResource(R.string.chat_format_italic),
                onClick = { formatAction { ChatFormatActions.wrapSelection(draft, onDraftChange, "_", "_") } },
            )
            FormatMenuItem(
                icon = Icons.Outlined.Code,
                text = stringResource(R.string.chat_format_mono),
                onClick = { formatAction { ChatFormatActions.wrapSelection(draft, onDraftChange, "`", "`") } },
            )
            FormatMenuItem(
                icon = Icons.Outlined.FormatStrikethrough,
                text = stringResource(R.string.chat_format_strike),
                onClick = { formatAction { ChatFormatActions.wrapSelection(draft, onDraftChange, "~~", "~~") } },
            )
            FormatMenuItem(
                icon = Icons.Outlined.FormatUnderlined,
                text = stringResource(R.string.chat_format_underline),
                onClick = { formatAction { ChatFormatActions.wrapSelection(draft, onDraftChange, "__", "__") } },
            )
            FormatMenuItem(
                icon = Icons.Outlined.Link,
                text = stringResource(R.string.chat_format_link),
                onClick = { formatAction { onShowLinkDialog() } },
            )
            FormatMenuItem(
                icon = Icons.Outlined.DataObject,
                text = stringResource(R.string.chat_format_code),
                onClick = {
                    formatAction {
                        val selected = draft.text.substring(draft.selection.min, draft.selection.max)
                        val block = "```\n${selected.ifBlank { "code" }}\n```"
                        ChatFormatActions.insertAtSelection(draft, onDraftChange, block)
                    }
                },
            )
            FormatMenuItem(
                icon = Icons.Outlined.FormatClear,
                text = stringResource(R.string.chat_format_regular),
                onClick = { formatAction { ChatFormatActions.stripFormatting(draft, onDraftChange) } },
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        )

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.chat_format_back))
        }
    }
}

@Composable
private fun FormatMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FormatMenuItemHeight),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(text)
        }
    }
}

@Composable
private fun SelectionTextActionButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(text)
    }
}
