package org.cyblight.android.data.home

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.cyblight.android.BuildConfig
import org.cyblight.android.R
import org.cyblight.android.i18n.LocaleManager
import org.cyblight.android.update.formatReleaseNotesForDisplay
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class HomeContentRepository(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val gson = Gson()

    suspend fun loadHomeContent(context: Context, locale: String): Result<HomeContent> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalizedLocale = locale.ifBlank { "ru" }
                val resources = LocaleManager.wrapContext(context, normalizedLocale).resources
                val projects = HomeContentDefaults.projects(resources, normalizedLocale)
                val whatsNew = HomeContentDefaults.whatsNew(resources)
                val fetched = fetchSiteContent(normalizedLocale)
                HomeContent(
                    siteNotice = fetched.siteNotice,
                    news = fetched.news.ifEmpty {
                        HomeContentDefaults.fallbackNews(resources, normalizedLocale)
                    },
                    projects = projects,
                    whatsNew = whatsNew,
                )
            }
        }

    suspend fun loadChangelog(): Result<List<ChangelogRelease>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/CybLight/CybLight-Android/releases?per_page=30")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "CybLight-Android/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("github_http_${response.code}")
                val body = response.body?.string() ?: error("empty_response")
                val releases = gson.fromJson(body, Array<GitHubRelease>::class.java) ?: emptyArray()
                releases.mapNotNull { release ->
                    val version = release.tagName?.removePrefix("v")?.trim().orEmpty()
                    if (version.isBlank()) return@mapNotNull null
                    ChangelogRelease(
                        version = version,
                        publishedAt = release.publishedAt.orEmpty(),
                        notes = formatReleaseNotesForDisplay(release.body?.trim().orEmpty()),
                    )
                }
            }
        }
    }

    private suspend fun fetchSiteContent(locale: String): FetchedSiteContent = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${BuildConfig.WEBSITE_URL}/$locale/")
                .header("User-Agent", "CybLight-Android/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching FetchedSiteContent()
                val html = response.body?.string().orEmpty()
                parseSiteHtml(html)
            }
        }.getOrElse { FetchedSiteContent() }
    }

    private fun parseSiteHtml(html: String): FetchedSiteContent {
        val notice = Regex(
            """<p class="hero-content-two"[^>]*>([\s\S]*?)</p>""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()

        val bannerNews = parseHeroNewsBanners(html)

        val videoSection = html.substringAfter("id=\"videos\"", html)
        val videoNews = Regex(
            """data-video-id="([^"]+)"[^>]*data-title="([^"]+)"""",
        ).findAll(videoSection).map { match ->
            val videoId = match.groupValues[1]
            val title = match.groupValues[2].trim()
            HomeNewsItem(
                title = title,
                url = "https://www.youtube.com/watch?v=$videoId",
                imageUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            )
        }.toList()

        val news = bannerNews + videoNews

        return FetchedSiteContent(siteNotice = notice, news = news)
    }

    private fun parseHeroNewsBanners(html: String): List<HomeNewsItem> {
        val blocks = Regex(
            """<aside class="hero-news-banner"[\s\S]*?</aside>""",
            RegexOption.IGNORE_CASE,
        ).findAll(html)

        return blocks.mapNotNull { match ->
            val block = match.value
            val title = extractClassText(block, "hero-news-title") ?: return@mapNotNull null
            val subtitle = extractClassText(block, "hero-news-text")
            val rawUrl = Regex(
                """class="[^"]*hero-news-cta[^"]*"[^>]*href="([^"]+)"""",
                RegexOption.IGNORE_CASE,
            ).find(block)?.groupValues?.get(1)?.trim().orEmpty()
            if (rawUrl.isEmpty()) return@mapNotNull null

            val rawImage = Regex(
                """class="hero-news-icon"[^>]*src="([^"]+)"""",
                RegexOption.IGNORE_CASE,
            ).find(block)?.groupValues?.get(1)?.trim()

            HomeNewsItem(
                title = title,
                subtitle = subtitle,
                url = resolveSiteUrl(rawUrl),
                imageUrl = rawImage?.let { resolveSiteUrl(it) },
            )
        }.toList()
    }

    private fun extractClassText(html: String, className: String): String? {
        return Regex(
            """class="$className"[^>]*>([\s\S]*?)</""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun resolveSiteUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val base = BuildConfig.WEBSITE_URL.trimEnd('/')
        return if (trimmed.startsWith("/")) "$base$trimmed" else "$base/$trimmed"
    }

    private data class FetchedSiteContent(
        val siteNotice: String? = null,
        val news: List<HomeNewsItem> = emptyList(),
    )

    private data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String?,
        @SerializedName("published_at") val publishedAt: String?,
        val body: String?,
    )
}

private object HomeContentDefaults {
    fun whatsNew(resources: android.content.res.Resources): WhatsNewInfo = WhatsNewInfo(
        version = BuildConfig.VERSION_NAME,
        features = resources.getStringArray(R.array.home_whats_new_features).toList(),
    )

    fun fallbackNews(
        resources: android.content.res.Resources,
        locale: String,
    ): List<HomeNewsItem> = listOf(
        HomeNewsItem(
            title = resources.getString(R.string.home_fallback_news_title),
            url = "${BuildConfig.WEBSITE_URL}/$locale/",
            imageUrl = "${BuildConfig.WEBSITE_URL}/images/hero.png",
        ),
    )

    fun projects(
        resources: android.content.res.Resources,
        locale: String,
    ): List<HomeProjectItem> {
        val base = BuildConfig.WEBSITE_URL
        val l = locale.ifBlank { "ru" }
        return listOf(
            HomeProjectItem(
                title = resources.getString(R.string.home_project_site_title),
                description = resources.getString(R.string.home_project_site_desc),
                imageUrl = "$base/images/hero.png",
                url = "$base/$l/projects/",
                tags = listOf("TypeScript", "HTML", "CSS"),
            ),
            HomeProjectItem(
                title = resources.getString(R.string.home_project_app_title),
                description = resources.getString(R.string.home_project_app_desc),
                imageUrl = "$base/images/favicon_192.png",
                url = "$base/$l/downloads/",
                tags = listOf("Kotlin", "Android"),
            ),
            HomeProjectItem(
                title = resources.getString(R.string.home_project_guardian_title),
                description = resources.getString(R.string.home_project_guardian_desc),
                imageUrl = "$base/images/project/Guardian_BOT.jpg",
                url = "$base/$l/projects/",
                tags = listOf("Python"),
            ),
            HomeProjectItem(
                title = resources.getString(R.string.home_project_pmx_title),
                description = resources.getString(R.string.home_project_pmx_desc),
                imageUrl = "$base/images/project/PmX-background.png",
                url = "$base/$l/projects/",
                tags = listOf("C#"),
            ),
            HomeProjectItem(
                title = resources.getString(R.string.home_project_smarthome_title),
                description = resources.getString(R.string.home_project_smarthome_desc),
                imageUrl = "$base/images/project/Smart%20Home%20Hub%20(640%20x%20360).png",
                url = "$base/$l/projects/",
                tags = listOf("Arduino", "C++"),
            ),
            HomeProjectItem(
                title = resources.getString(R.string.home_project_fishfinder_title),
                description = resources.getString(R.string.home_project_fishfinder_desc),
                imageUrl = "$base/images/project/Fish%20Finder%20PRO.jpg",
                url = "$base/$l/projects/",
                tags = listOf("Arduino", "C++"),
            ),
        )
    }
}
