package org.cyblight.android.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.cyblight.android.R
import org.cyblight.android.data.home.HomeContent
import org.cyblight.android.data.home.HomeNewsItem
import org.cyblight.android.data.home.HomeProjectItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    content: HomeContent?,
    isLoading: Boolean,
    error: String?,
    whatsNewBannerHidden: Boolean,
    onDismissWhatsNewBanner: () -> Unit,
    onRefresh: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenChangelog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && content == null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!whatsNewBannerHidden) {
                    item {
                        WhatsNewBanner(
                            content = content,
                            onOpenChangelog = onOpenChangelog,
                            onDismiss = onDismissWhatsNewBanner,
                        )
                    }
                }

                if (!content?.siteNotice.isNullOrBlank()) {
                    item {
                        SectionTitle(stringResource(R.string.home_site_news))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = content?.siteNotice.orEmpty(),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                item {
                    SectionTitle(stringResource(R.string.home_projects))
                    ProjectsCarousel(
                        projects = content?.projects.orEmpty(),
                        onOpenUrl = onOpenUrl,
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.home_news))
                }

                if (content?.news.isNullOrEmpty()) {
                    item {
                        Text(
                            text = error?.let { stringResource(R.string.home_news_error) }
                                ?: stringResource(R.string.home_news_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onRefresh() },
                        )
                    }
                } else {
                    items(content?.news.orEmpty(), key = { it.url }) { item ->
                        NewsCard(item = item, onOpenUrl = onOpenUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun WhatsNewBanner(
    content: HomeContent?,
    onOpenChangelog: () -> Unit,
    onDismiss: () -> Unit,
) {
    val whatsNew = content?.whatsNew
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Box {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                )
            }
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 40.dp,
                    top = 16.dp,
                    bottom = 16.dp,
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.home_whats_new_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
                if (!whatsNew?.version.isNullOrBlank()) {
                    Text(
                        text = "v${whatsNew?.version}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            whatsNew?.features?.forEach { feature ->
                Text(
                    text = "• $feature",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            TextButton(
                onClick = onOpenChangelog,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.home_previous_versions))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp),
                )
            }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectsCarousel(
    projects: List<HomeProjectItem>,
    onOpenUrl: (String) -> Unit,
) {
    if (projects.isEmpty()) {
        Text(
            text = stringResource(R.string.home_projects_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { projects.size })
    val scope = rememberCoroutineScope()

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val project = projects[page]
            ProjectCard(project = project, onOpenUrl = onOpenUrl)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = pagerState.currentPage > 0,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
            ) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = null)
            }
            Text(
                text = "${pagerState.currentPage + 1} / ${projects.size}",
                style = MaterialTheme.typography.labelMedium,
            )
            IconButton(
                enabled = pagerState.currentPage < projects.lastIndex,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: HomeProjectItem,
    onOpenUrl: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(project.url) },
    ) {
        AsyncImage(
            model = project.imageUrl,
            contentDescription = project.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (project.tags.isNotEmpty()) {
                Text(
                    text = project.tags.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun NewsCard(
    item: HomeNewsItem,
    onOpenUrl: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(item.url) },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (item.imageUrl.isNullOrBlank()) 0.dp else 12.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.subtitle.isNullOrBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.home_open_on_site),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
