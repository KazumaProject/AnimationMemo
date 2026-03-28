package com.kazumaproject.animationswipememo.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class LinkPreviewMetadataFetcher {
    suspend fun fetch(url: String): LinkPreviewMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = normalizeUrl(url) ?: return@runCatching null
            val document = Jsoup.connect(normalizedUrl)
                .userAgent(DEFAULT_USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MS)
                .followRedirects(true)
                .get()
            parse(document)
        }.getOrNull()
    }

    internal fun parse(document: Document): LinkPreviewMetadata {
        val title = firstNonBlank(
            document.select("meta[property=og:title]").attr("content"),
            document.title()
        )
        val description = firstNonBlank(
            document.select("meta[property=og:description]").attr("content"),
            document.select("meta[name=description]").attr("content")
        )
        val imageUrl = firstNonBlank(
            absoluteAttr(document, "meta[property=og:image]", "content"),
            absoluteAttr(document, "meta[name=twitter:image]", "content")
        )
        val faviconUrl = firstNonBlank(
            absoluteAttr(document, "link[rel~=^(?i)(shortcut\\s+icon|icon)$]", "href"),
            absoluteAttr(document, "link[rel=apple-touch-icon]", "href"),
            absoluteAttr(document, "link[rel=apple-touch-icon-precomposed]", "href")
        )

        return LinkPreviewMetadata(
            title = title,
            description = description,
            imageUrl = imageUrl,
            faviconUrl = faviconUrl
        )
    }

    private fun absoluteAttr(document: Document, selector: String, attr: String): String {
        val element = document.selectFirst(selector) ?: return ""
        return firstNonBlank(element.absUrl(attr), element.attr(attr))
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }

    companion object {
        internal fun normalizeUrl(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed
            } else {
                "https://$trimmed"
            }
            return withScheme.takeIf { it.contains('.') }
        }

        private const val REQUEST_TIMEOUT_MS = 8_000
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0 Mobile Safari/537.36"
    }
}

data class LinkPreviewMetadata(
    val title: String,
    val description: String,
    val imageUrl: String,
    val faviconUrl: String
)

