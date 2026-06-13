package org.cyblight.android.data.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.cyblight.android.BuildConfig
import org.cyblight.android.update.formatReleaseNotesForDisplay
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class HomeContentRepository(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val gson = Gson()

    suspend fun loadHomeContent(locale: String): Result<HomeContent> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedLocale = locale.ifBlank { "ru" }
            val projects = HomeContentDefaults.projects(normalizedLocale)
            val whatsNew = HomeContentDefaults.whatsNew()
            val fetched = fetchSiteContent(normalizedLocale)
            HomeContent(
                siteNotice = fetched.siteNotice,
                news = fetched.news.ifEmpty { HomeContentDefaults.fallbackNews(normalizedLocale) },
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
                parseSiteHtml(html, locale)
            }
        }.getOrElse { FetchedSiteContent() }
    }

    private fun parseSiteHtml(html: String, locale: String): FetchedSiteContent {
        val notice = Regex(
            """<p class="hero-content-two"[^>]*>([\s\S]*?)</p>""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()

        val videoSection = html.substringAfter("id=\"videos\"", html)
        val news = Regex(
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

        return FetchedSiteContent(siteNotice = notice, news = news)
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
    fun whatsNew(): WhatsNewInfo = WhatsNewInfo(
        version = BuildConfig.VERSION_NAME,
        features = listOf(
            "Вкладка «Главная» с новостями и проектами CybLight",
            "Превью последних сообщений и реакций в списке чатов",
            "Черновики сообщений при выходе из чата",
            "Прогресс пасхалок и карточки как на сайте",
            "Управление жестами: свайп назад и настройки в параметрах",
        ),
    )

    fun fallbackNews(locale: String): List<HomeNewsItem> = listOf(
        HomeNewsItem(
            title = when (locale) {
                "en" -> "CybLight website"
                "uk" -> "Сайт CybLight"
                else -> "Сайт CybLight"
            },
            url = "${BuildConfig.WEBSITE_URL}/$locale/",
            imageUrl = "${BuildConfig.WEBSITE_URL}/images/hero.png",
        ),
    )

    fun projects(locale: String): List<HomeProjectItem> {
        val base = BuildConfig.WEBSITE_URL
        val l = locale.ifBlank { "ru" }
        return listOf(
            HomeProjectItem(
                title = "Сайт CybLight",
                description = "Мой основной сайт с проектами, блогом и играми.",
                imageUrl = "$base/images/hero.png",
                url = "$base/$l/projects/",
                tags = listOf("TypeScript", "HTML", "CSS"),
            ),
            HomeProjectItem(
                title = "Мобильное приложение CybLight",
                description = "Официальное Android-приложение: аккаунт, друзья, сообщения и безопасность.",
                imageUrl = "$base/images/favicon_192.png",
                url = "$base/$l/downloads/",
                tags = listOf("Kotlin", "Android"),
            ),
            HomeProjectItem(
                title = "Telegram-бот Guardian",
                description = "Бот для поддержания порядка в группах.",
                imageUrl = "$base/images/project/Guardian_BOT.jpg",
                url = "$base/$l/projects/",
                tags = listOf("Python"),
            ),
            HomeProjectItem(
                title = "Priority Manager X",
                description = "Управление приоритетами процессов и правила для Windows.",
                imageUrl = "$base/images/project/PmX-background.png",
                url = "$base/$l/projects/",
                tags = listOf("C#"),
            ),
            HomeProjectItem(
                title = "Smart Home Hub",
                description = "Оптимизация и уют для дома через умные технологии.",
                imageUrl = "$base/images/project/Smart%20Home%20Hub%20(640%20x%20360).png",
                url = "$base/$l/projects/",
                tags = listOf("Arduino", "C++"),
            ),
            HomeProjectItem(
                title = "Fish Finder PRO",
                description = "Эхолот для рыбалки на базе Arduino.",
                imageUrl = "$base/images/project/Fish%20Finder%20PRO.jpg",
                url = "$base/$l/projects/",
                tags = listOf("Arduino", "C++"),
            ),
        )
    }
}
