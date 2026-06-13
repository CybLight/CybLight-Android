package org.cyblight.android.data.home

data class HomeNewsItem(
    val title: String,
    val url: String,
    val imageUrl: String? = null,
    val subtitle: String? = null,
)

data class HomeProjectItem(
    val title: String,
    val description: String,
    val imageUrl: String,
    val url: String,
    val tags: List<String> = emptyList(),
)

data class WhatsNewInfo(
    val version: String,
    val features: List<String>,
)

data class HomeContent(
    val siteNotice: String? = null,
    val news: List<HomeNewsItem> = emptyList(),
    val projects: List<HomeProjectItem> = emptyList(),
    val whatsNew: WhatsNewInfo,
)

data class ChangelogRelease(
    val version: String,
    val publishedAt: String,
    val notes: String,
)
