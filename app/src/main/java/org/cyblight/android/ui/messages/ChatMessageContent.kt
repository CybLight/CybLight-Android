package org.cyblight.android.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.cyblight.android.R
import org.cyblight.android.data.repository.LinkPreviewData
import org.cyblight.android.data.repository.LinkPreviewRepository
import org.cyblight.android.util.ExternalLinks

@Composable
fun ChatMessageContent(
    rawContent: String,
    textColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
    messageId: String,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(rawContent, linkColor) {
        ChatMessageParser.parseWithSpoilers(rawContent, linkColor)
    }
    val revealedSpoilers = remember { mutableStateMapOf<String, Boolean>() }
    val previewRepository = remember { LinkPreviewRepository() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parsed.parts.forEach { part ->
            when (part) {
                is ChatMessagePart.Text -> {
                    if (part.content.text.isNotBlank()) {
                        Text(
                            text = part.content,
                            style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                        )
                    }
                }
                is ChatMessagePart.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                            .padding(8.dp),
                    ) {
                        if (part.language.isNotBlank()) {
                            Text(
                                part.language,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f),
                            )
                        }
                        Text(
                            part.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = textColor,
                            ),
                        )
                    }
                }
                is ChatMessagePart.Spoiler -> {
                    val key = "$messageId-spoiler-${part.id}"
                    val revealed = revealedSpoilers[key] == true
                    Text(
                        text = if (revealed) part.text else "▒".repeat(part.text.length.coerceIn(3, 16)),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = if (revealed) FontFamily.Default else FontFamily.Monospace,
                            color = textColor,
                        ),
                        modifier = Modifier.clickable { revealedSpoilers[key] = true },
                    )
                }
            }
        }

        parsed.previewUrl?.let { url ->
            LinkPreviewCard(
                url = url,
                repository = previewRepository,
                textColor = textColor,
            )
        }
    }
}

@Composable
private fun LinkPreviewCard(
    url: String,
    repository: LinkPreviewRepository,
    textColor: androidx.compose.ui.graphics.Color,
) {
    val context = LocalContext.current
    var preview by remember(url) { mutableStateOf<LinkPreviewData?>(null) }
    var loading by remember(url) { mutableStateOf(true) }

    LaunchedEffect(url) {
        loading = true
        preview = repository.fetch(url)
        loading = false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
            .clickable { ExternalLinks.openUrl(context, url) }
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.chat_link_preview_loading),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                val title = preview?.title?.ifBlank { preview?.siteName }?.ifBlank { url }
                Text(
                    title.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 2,
                )
                preview?.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = 2,
                    )
                }
                Text(
                    runCatching { java.net.URI(url).host }.getOrDefault(url),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.65f),
                    maxLines = 1,
                )
            }
        }
        if (!loading && !preview?.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(preview?.imageUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun ComposeLinkPreview(
    draft: String,
    suppressedUrl: String?,
    onSuppress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val url = ChatFormatUtils.extractFirstUrl(draft) ?: return
    if (suppressedUrl == url) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.chat_compose_preview_title),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                runCatching {
                    val uri = java.net.URI(url)
                    "${uri.host}${uri.path.orEmpty()}"
                }.getOrDefault(url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Text(
            text = "✕",
            modifier = Modifier
                .clickable(onClick = onSuppress)
                .padding(8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
