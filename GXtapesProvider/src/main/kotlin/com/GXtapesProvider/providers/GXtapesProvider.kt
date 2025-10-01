package com.GXtapesProvider.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * GXtapes provider that fetches ALL pages for Latest, every Category and every Channel.
 * Designed to compile against the recloudstream/TestPlugins template.
 */
class GXtapesProvider : MainAPI() {
    override var mainUrl = "https://gay.xtapes.in"
    override var name = "GXtapes (All)"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.NSFW)

    // -------------------- helpers --------------------

    private fun Element.toCard(): SearchResponse? {
        val a = selectFirst("a[href*=/video/]") ?: return null
        val href = fixUrl(a.attr("href"))
        val title = a.attr("title").ifBlank { selectFirst(".thumb-title, .title")?.text() ?: "Video" }
        val poster = selectFirst("img")?.attr("data-src") ?: selectFirst("img")?.attr("src")
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    private fun listCards(doc: Document): List<SearchResponse> =
        doc.select("div.item, .thumb, .video, .thumb-block, .video-item")
            .mapNotNull { it.toCard() }

    private suspend fun findNext(doc: Document): String? =
        doc.selectFirst("a.next, a[rel=next], .pagination a:matchesOwn(Next), .nav-links a.next")
            ?.attr("href")?.let { fixUrl(it) }

    private suspend fun paginateAll(startUrl: String, cap: Int = 500): List<SearchResponse> {
        val out = ArrayList<SearchResponse>(512)
        val seen = HashSet<String>()
        var url: String? = startUrl
        var hops = 0
        while (url != null && hops < cap) {
            if (!seen.add(url)) break
            val doc = app.get(url).document
            out += listCards(doc)
            url = findNext(doc)
            hops++
        }
        return out
    }

    // -------------------- main page --------------------

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val sections = mutableListOf<HomePageList>()

        // Latest
        val latestAll = paginateAll("$mainUrl/latest/")
        sections += HomePageList("Latest (ALL)", latestAll)

        // Categories
        val catsDoc = app.get("$mainUrl/categories/").document
        val catLinks = catsDoc.select("a[href*=/category/], a[href*=/categories/]")
            .mapNotNull { it.attr("href") }.map { fixUrl(it) }.distinct()
        val catAgg = mutableListOf<SearchResponse>()
        for (c in catLinks) catAgg += paginateAll(c)
        sections += HomePageList("Categories (ALL)", catAgg)

        // Channels / Models
        val chanDoc = app.get("$mainUrl/channels/").document
        val chanLinks = chanDoc.select("a[href*=/channel/], a[href*=/pornstar/], a[href*=/model/]")
            .mapNotNull { it.attr("href") }.map { fixUrl(it) }.distinct()
        val chanAgg = mutableListOf<SearchResponse>()
        for (ch in chanLinks) chanAgg += paginateAll(ch)
        sections += HomePageList("Channels (ALL)", chanAgg)

        return newHomePageResponse(sections)
    }

    // -------------------- item load --------------------
    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1, .title")?.text() ?: name
        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst("video, .player video")?.attr("poster")
        val tags = doc.select("a[href*=/tag/], .tags a").map { it.text() }
        val year = doc.selectFirst(".date, time, meta[itemprop=datePublished]")?.text()
            ?.takeLast(4)?.toIntOrNull()

        val links = ArrayList<ExtractorLink>()

        // direct <source> elements
        doc.select("source[src]").forEach { s ->
            val src = s.attr("src")
            if (src.isNullOrBlank()) return@forEach
            val q = getQualityFromName(
                listOf(s.attr("label"), s.attr("res"), s.attr("size"))
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            )
            links += newExtractorLink(
                source = this.name,
                name = this.name,
                url = fixUrl(src),
            ) {
                this.referer = url
                this.quality = q
                this.type = if (src.endsWith(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            }
        }

        // iframe players via generic extractors
        doc.select("iframe[src]").map { it.attr("src") }.distinct().forEach { frame ->
            loadExtractor(
                fixUrl(frame),
                url,
                subtitleCallback = { /* ignore subs */ },
                callback = { link -> links.add(link) }
            )
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, links) {
            this.posterUrl = poster
            this.tags = tags
            this.year = year
        }
    }
    // -------------------- search --------------------

    override suspend fun search(query: String): List<SearchResponse> {
        suspend fun pageAll(startUrl: String, cap: Int = 500): List<SearchResponse> {
            val out = ArrayList<SearchResponse>()
            val seen = HashSet<String>()
            var url: String? = startUrl
            var hops = 0
            while (url != null && hops < cap) {
                if (!seen.add(url)) break
                val doc = app.get(url).document
                out += listCards(doc)
                url = findNext(doc)
                hops++
            }
            return out
        }
        val url = "$mainUrl/?s=" + query.replace(" ", "+")
        return pageAll(url)
    }
}
