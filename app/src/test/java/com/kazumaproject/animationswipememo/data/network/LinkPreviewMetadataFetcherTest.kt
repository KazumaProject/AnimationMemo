package com.kazumaproject.animationswipememo.data.network

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkPreviewMetadataFetcherTest {
    private val fetcher = LinkPreviewMetadataFetcher()

    @Test
    fun parse_prefersOgTags_andResolvesRelativeUrls() {
        val document = Jsoup.parse(
            """
            <html>
              <head>
                <title>Fallback title</title>
                <meta property="og:title" content="OG Title" />
                <meta property="og:description" content="OG Description" />
                <meta property="og:image" content="/images/hero.png" />
                <link rel="icon" href="/favicon.ico" />
              </head>
              <body></body>
            </html>
            """.trimIndent(),
            "https://example.com/article"
        )

        val metadata = fetcher.parse(document)

        assertEquals("OG Title", metadata.title)
        assertEquals("OG Description", metadata.description)
        assertEquals("https://example.com/images/hero.png", metadata.imageUrl)
        assertEquals("https://example.com/favicon.ico", metadata.faviconUrl)
    }

    @Test
    fun parse_fallsBackToStandardTags_whenOgTagsAreMissing() {
        val document = Jsoup.parse(
            """
            <html>
              <head>
                <title>Page Title</title>
                <meta name="description" content="Page description" />
                <meta name="twitter:image" content="https://cdn.example.com/cover.jpg" />
              </head>
              <body></body>
            </html>
            """.trimIndent(),
            "https://example.com"
        )

        val metadata = fetcher.parse(document)

        assertEquals("Page Title", metadata.title)
        assertEquals("Page description", metadata.description)
        assertEquals("https://cdn.example.com/cover.jpg", metadata.imageUrl)
        assertEquals("", metadata.faviconUrl)
    }

    @Test
    fun normalizeUrl_addsHttps_whenSchemeIsMissing() {
        assertEquals("https://example.com/path", LinkPreviewMetadataFetcher.normalizeUrl("example.com/path"))
    }

    @Test
    fun normalizeUrl_returnsNull_forInvalidOrBlankInput() {
        assertNull(LinkPreviewMetadataFetcher.normalizeUrl(""))
        assertNull(LinkPreviewMetadataFetcher.normalizeUrl("localhost"))
    }
}

