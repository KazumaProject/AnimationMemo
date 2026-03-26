package com.kazumaproject.animationswipememo.ui.components.render

import com.itsaky.androidide.treesitter.TSLanguage
import com.itsaky.androidide.treesitter.TSNode
import com.itsaky.androidide.treesitter.TSParser
import com.itsaky.androidide.treesitter.TreeSitter
import com.itsaky.androidide.treesitter.c.TSLanguageC
import com.itsaky.androidide.treesitter.cpp.TSLanguageCpp
import com.itsaky.androidide.treesitter.java.TSLanguageJava
import com.itsaky.androidide.treesitter.json.TSLanguageJson
import com.itsaky.androidide.treesitter.kotlin.TSLanguageKotlin
import com.itsaky.androidide.treesitter.python.TSLanguagePython
import com.itsaky.androidide.treesitter.xml.TSLanguageXml
import com.kazumaproject.animationswipememo.platform.AppContextHolder

internal object TreeSitterBridge {
    private val languageProviders: Map<String, () -> TSLanguage> = mapOf(
        "kotlin" to { TSLanguageKotlin.getInstance() },
        "java" to { TSLanguageJava.getInstance() },
        "python" to { TSLanguagePython.getInstance() },
        "json" to { TSLanguageJson.getInstance() },
        "xml" to { TSLanguageXml.getInstance() },
        "c" to { TSLanguageC.getInstance() },
        "cpp" to { TSLanguageCpp.getInstance() }
    )

    private val keywordNodeNames = setOf(
        "if", "else", "for", "while", "return", "class", "interface", "object", "fun",
        "def", "import", "package", "public", "private", "protected", "static", "new",
        "when", "match", "switch", "case", "break", "continue", "try", "catch", "throw"
    )

    // These are load-at-runtime grammars (jniLibs/.so) expected names.
    private val dynamicGrammarNames: Map<String, String> = mapOf(
        "javascript" to "javascript",
        "typescript" to "typescript",
        "tsx" to "tsx",
        "html" to "html",
        "css" to "css",
        "shell" to "bash",
        "yaml" to "yaml",
        "sql" to "sql",
        "markdown" to "markdown",
        "toml" to "toml",
        "ruby" to "ruby",
        "go" to "go",
        "rust" to "rust",
        "swift" to "swift",
        "php" to "php",
        "dart" to "dart",
        "protobuf" to "proto",
        "nginx" to "nginx"
    )

    private val dynamicLanguageCache = mutableMapOf<String, TSLanguage>()

    fun tokenize(language: String, code: String): List<TokenCategorySpan>? {
        if (language.isBlank() || code.isBlank()) return null
        val canonicalLanguage = canonicalLanguage(language)
        val provider = languageProviders[canonicalLanguage]

        return runCatching {
            TreeSitter.loadLibrary()
            val parser = TSParser.create()
            val parsedSpans = mutableListOf<TokenCategorySpan>()
            try {
                val resolvedLanguage = provider?.invoke() ?: loadDynamicLanguage(canonicalLanguage)
                    ?: return@runCatching null
                parser.setLanguage(resolvedLanguage)
                val tree = parser.parseString(code) ?: return@runCatching null
                try {
                    collectNodeSpans(node = tree.getRootNode(), out = parsedSpans)
                } finally {
                    tree.close()
                }
            } finally {
                parser.close()
            }

            parsedSpans
                .filter { it.start in 0..code.length && it.endExclusive in 0..code.length && it.start < it.endExclusive }
                .distinctBy { Triple(it.start, it.endExclusive, it.category) }
                .sortedBy { it.start }
        }.getOrNull()
    }

    private fun collectNodeSpans(node: TSNode, out: MutableList<TokenCategorySpan>) {
        if (node.isNull()) return

        val nodeType = node.getType().lowercase()
        val start = node.getStartByte().coerceAtLeast(0)
        val end = node.getEndByte().coerceAtLeast(start)

        when {
            nodeType.contains("comment") -> {
                out += TokenCategorySpan(start = start, endExclusive = end, category = TokenCategory.Comment)
            }

            nodeType.contains("string") || nodeType.contains("char") -> {
                out += TokenCategorySpan(start = start, endExclusive = end, category = TokenCategory.StringLiteral)
            }

            nodeType.endsWith("_keyword") || nodeType in keywordNodeNames -> {
                out += TokenCategorySpan(start = start, endExclusive = end, category = TokenCategory.Keyword)
            }
        }

        for (index in 0 until node.getChildCount()) {
            collectNodeSpans(node = node.getChild(index), out = out)
        }
    }

    private fun canonicalLanguage(language: String): String {
        val lowered = language.trim().lowercase()
        return when (lowered) {
            "js", "java script" -> "javascript"
            "ts" -> "typescript"
            "bash", "sh", "zsh", "bash / shell" -> "shell"
            "xml", "xhtml" -> "xml"
            "yml" -> "yaml"
            "diff / patch" -> "diff"
            "objective-c" -> "objc"
            "c++" -> "cpp"
            "plain text", "text" -> "plaintext"
            else -> lowered
        }
    }

    private fun loadDynamicLanguage(canonicalLanguage: String): TSLanguage? {
        dynamicLanguageCache[canonicalLanguage]?.let { return it }
        val grammarName = dynamicGrammarNames[canonicalLanguage] ?: return null
        val context = AppContextHolder.get() ?: return null
        val loaded = runCatching {
            // Resolves libtree-sitter-<grammarName>.so from packaged native libs.
            TSLanguage.loadLanguage(context, grammarName)
        }.getOrNull() ?: return null
        dynamicLanguageCache[canonicalLanguage] = loaded
        return loaded
    }
}

internal data class TokenCategorySpan(
    val start: Int,
    val endExclusive: Int,
    val category: TokenCategory
)

internal enum class TokenCategory {
    Keyword,
    StringLiteral,
    Comment
}

