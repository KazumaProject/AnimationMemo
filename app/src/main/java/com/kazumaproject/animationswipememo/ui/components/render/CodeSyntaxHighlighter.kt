package com.kazumaproject.animationswipememo.ui.components.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private data class LanguageSpec(
    val keywords: Set<String>,
    val lineCommentStarts: List<String> = emptyList(),
    val blockCommentPairs: List<Pair<String, String>> = emptyList()
)

private val hashLineComment = listOf("#")
private val slashLineComment = listOf("//")
private val sqlLineComment = listOf("--")
private val semicolonLineComment = listOf(";")
private val slashBlockComment = listOf("/*" to "*/")
private val markupBlockComment = listOf("<!--" to "-->")

private val languageAliases = mapOf(
    "plain text" to "plaintext",
    "text" to "plaintext",
    "bash / shell" to "shell",
    "bash" to "shell",
    "sh" to "shell",
    "zsh" to "shell",
    "shell" to "shell",
    "js" to "javascript",
    "java script" to "javascript",
    "ts" to "typescript",
    "jsx" to "jsx",
    "tsx" to "tsx",
    "type script" to "typescript",
    "yml" to "yaml",
    "toml" to "toml",
    "ini" to "ini",
    "ini / toml" to "toml",
    "diff / patch" to "diff",
    "patch" to "patch",
    "markdown" to "markdown",
    "md" to "markdown",
    "c++" to "cpp",
    "c#" to "csharp",
    "objective-c" to "objectivec",
    "lisp / clojure" to "clojure",
    "powershell" to "powershell",
    "nginx config" to "nginx",
    "dockerfile" to "dockerfile"
)

private val languageSpecs = mapOf(
    "plaintext" to LanguageSpec(emptySet(), emptyList()),
    "json" to LanguageSpec(setOf("true", "false", "null")),
    "javascript" to LanguageSpec(setOf("function", "const", "let", "var", "if", "else", "for", "while", "return", "import", "export", "class", "new", "async", "await", "try", "catch"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "typescript" to LanguageSpec(setOf("function", "const", "let", "var", "if", "else", "for", "while", "return", "import", "export", "class", "interface", "type", "enum", "implements", "extends", "async", "await"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "html" to LanguageSpec(setOf("div", "span", "script", "style", "body", "head"), blockCommentPairs = markupBlockComment),
    "css" to LanguageSpec(setOf("display", "position", "color", "background", "font", "grid", "flex"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "markdown" to LanguageSpec(setOf("#", "##", "###"), emptyList()),
    "shell" to LanguageSpec(setOf("if", "then", "fi", "for", "do", "done", "case", "esac", "function", "export"), lineCommentStarts = hashLineComment),
    "python" to LanguageSpec(setOf("def", "class", "if", "elif", "else", "for", "while", "return", "import", "from", "as", "try", "except", "with", "lambda"), lineCommentStarts = hashLineComment),
    "sql" to LanguageSpec(setOf("select", "from", "where", "join", "insert", "update", "delete", "group", "order", "by", "limit", "and", "or"), lineCommentStarts = sqlLineComment, blockCommentPairs = slashBlockComment),
    "yaml" to LanguageSpec(setOf("true", "false", "null"), lineCommentStarts = hashLineComment),
    "xml" to LanguageSpec(setOf("xml", "version", "encoding"), blockCommentPairs = markupBlockComment),
    "jsx" to LanguageSpec(setOf("function", "const", "let", "return", "import", "export", "className"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "tsx" to LanguageSpec(setOf("function", "const", "let", "return", "import", "export", "interface", "type", "className"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "kotlin" to LanguageSpec(setOf("fun", "val", "var", "class", "object", "when", "if", "else", "for", "while", "return", "import", "package", "private", "public", "internal", "data", "sealed"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "java" to LanguageSpec(setOf("class", "interface", "public", "private", "protected", "static", "void", "if", "else", "for", "while", "return", "new", "import", "package"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "c" to LanguageSpec(setOf("int", "char", "void", "if", "else", "for", "while", "return", "struct", "typedef", "include"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "cpp" to LanguageSpec(setOf("class", "template", "typename", "auto", "constexpr", "namespace", "if", "else", "for", "while", "return", "include"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "csharp" to LanguageSpec(setOf("class", "interface", "public", "private", "protected", "static", "void", "var", "if", "else", "for", "while", "return", "using", "namespace"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "go" to LanguageSpec(setOf("func", "package", "import", "var", "const", "type", "struct", "interface", "if", "else", "for", "range", "return", "go", "defer"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "rust" to LanguageSpec(setOf("fn", "let", "mut", "struct", "enum", "impl", "trait", "if", "else", "for", "while", "match", "return", "use", "pub"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "swift" to LanguageSpec(setOf("func", "let", "var", "class", "struct", "enum", "protocol", "if", "else", "for", "while", "return", "import", "guard"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "php" to LanguageSpec(setOf("function", "class", "public", "private", "protected", "if", "else", "foreach", "while", "return", "namespace", "use"), lineCommentStarts = listOf("//", "#"), blockCommentPairs = slashBlockComment),
    "ruby" to LanguageSpec(setOf("def", "class", "module", "if", "elsif", "else", "end", "do", "while", "return", "require"), lineCommentStarts = hashLineComment),
    "dart" to LanguageSpec(setOf("void", "class", "enum", "if", "else", "for", "while", "return", "import", "final", "const", "var"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "dockerfile" to LanguageSpec(setOf("from", "run", "cmd", "copy", "add", "entrypoint", "env", "arg", "workdir", "expose"), lineCommentStarts = hashLineComment),
    "ini" to LanguageSpec(setOf("true", "false", "on", "off"), lineCommentStarts = semicolonLineComment + hashLineComment),
    "toml" to LanguageSpec(setOf("true", "false"), lineCommentStarts = hashLineComment),
    "diff" to LanguageSpec(setOf("@@", "+++", "---"), emptyList()),
    "patch" to LanguageSpec(setOf("@@", "+++", "---"), emptyList()),
    "scala" to LanguageSpec(setOf("def", "val", "var", "class", "object", "trait", "if", "else", "for", "while", "match", "import", "package"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "haskell" to LanguageSpec(setOf("data", "type", "where", "let", "in", "if", "then", "else", "import", "module"), lineCommentStarts = sqlLineComment),
    "lua" to LanguageSpec(setOf("function", "local", "if", "then", "elseif", "else", "for", "while", "return", "end"), lineCommentStarts = sqlLineComment),
    "perl" to LanguageSpec(setOf("sub", "my", "our", "if", "elsif", "else", "for", "while", "return", "use"), lineCommentStarts = hashLineComment),
    "r" to LanguageSpec(setOf("function", "if", "else", "for", "while", "return", "library"), lineCommentStarts = hashLineComment),
    "objectivec" to LanguageSpec(setOf("@interface", "@implementation", "@property", "if", "else", "for", "while", "return", "import"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "groovy" to LanguageSpec(setOf("def", "class", "interface", "if", "else", "for", "while", "return", "import", "package"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "makefile" to LanguageSpec(setOf("if", "else", "endif", "include"), lineCommentStarts = hashLineComment),
    "nginx" to LanguageSpec(setOf("server", "location", "listen", "proxy_pass", "root", "try_files", "if"), lineCommentStarts = hashLineComment),
    "graphql" to LanguageSpec(setOf("query", "mutation", "subscription", "fragment", "type", "input", "schema"), lineCommentStarts = hashLineComment),
    "protobuf" to LanguageSpec(setOf("syntax", "package", "message", "enum", "service", "rpc", "import", "option"), lineCommentStarts = slashLineComment, blockCommentPairs = slashBlockComment),
    "assembly" to LanguageSpec(setOf("mov", "push", "pop", "call", "jmp", "cmp", "add", "sub", "ret"), lineCommentStarts = semicolonLineComment + hashLineComment),
    "lisp" to LanguageSpec(setOf("defun", "lambda", "let", "if", "cond", "car", "cdr", "setq"), lineCommentStarts = semicolonLineComment),
    "clojure" to LanguageSpec(setOf("defn", "let", "if", "cond", "fn", "ns", "require"), lineCommentStarts = semicolonLineComment),
    "powershell" to LanguageSpec(setOf("function", "param", "if", "else", "foreach", "return", "switch", "try", "catch"), lineCommentStarts = hashLineComment)
)

private val supportedLanguageLabels = listOf(
    "Plain Text", "JSON", "JavaScript", "TypeScript", "HTML", "CSS", "Markdown", "Bash / Shell", "Python", "SQL", "YAML", "XML",
    "JSX", "TSX", "Kotlin", "Java", "C", "C++", "C#", "Go", "Rust", "Swift", "PHP", "Ruby", "Dart", "Dockerfile", "ini / toml", "diff / patch",
    "Scala", "Haskell", "Lua", "Perl", "R", "Objective-C", "Groovy", "Makefile", "Nginx config", "GraphQL", "Protobuf", "Assembly", "Lisp / Clojure", "PowerShell"
)

private data class TokenSpan(
    val start: Int,
    val end: Int,
    val style: SpanStyle
)

fun highlightCode(
    language: String,
    code: String,
    defaultColor: Color,
    keywordColor: Color = Color(0xFF7C4DFF),
    stringColor: Color = Color(0xFF2E7D32),
    commentColor: Color = Color(0xFF78909C)
): AnnotatedString {
    if (code.isBlank()) return AnnotatedString(code)

    val spans = mutableListOf<TokenSpan>()
    val canonicalLanguage = canonicalLanguage(language)
    val spec = languageSpecs[canonicalLanguage] ?: languageSpecs.getValue("plaintext")
    val keywords = spec.keywords

    if (canonicalLanguage == "plaintext") {
        return AnnotatedString(code)
    }

    if (canonicalLanguage == "diff" || canonicalLanguage == "patch") {
        spans += lexDiffHeaders(code, keywordColor, commentColor)
    } else {
        val treeSitterSpans = TreeSitterBridge.tokenize(language = canonicalLanguage, code = code)
            ?.map { token ->
                val style = when (token.category) {
                    TokenCategory.Keyword -> SpanStyle(color = keywordColor)
                    TokenCategory.StringLiteral -> SpanStyle(color = stringColor)
                    TokenCategory.Comment -> SpanStyle(color = commentColor)
                }
                TokenSpan(token.start, token.endExclusive, style)
            }
            .orEmpty()
        spans += if (treeSitterSpans.isNotEmpty()) {
            treeSitterSpans
        } else {
            lexCode(
                code = code,
                spec = spec,
                keywords = keywords,
                keywordColor = keywordColor,
                stringColor = stringColor,
                commentColor = commentColor
            )
        }
    }

    val merged = spans
        .sortedBy { it.start }
        .fold(mutableListOf<TokenSpan>()) { acc, span ->
            val intersects = acc.any { existing -> span.start < existing.end && existing.start < span.end }
            if (!intersects) {
                acc += span
            } else {
                val replaceAt = acc.indexOfFirst { existing -> span.start >= existing.start && span.end <= existing.end }
                if (replaceAt >= 0) {
                    acc[replaceAt] = span
                }
            }
            acc
        }

    return buildAnnotatedString {
        pushStyle(SpanStyle(color = defaultColor))
        append(code)
        pop()
        merged.forEach { span ->
            addStyle(span.style, span.start, span.end)
        }
    }
}

fun supportedCodeLanguages(): List<String> = supportedLanguageLabels

private fun canonicalLanguage(input: String): String {
    val lowered = input.trim().lowercase()
    if (lowered.isBlank()) return "plaintext"
    return languageAliases[lowered] ?: lowered
}

private fun lexCode(
    code: String,
    spec: LanguageSpec,
    keywords: Set<String>,
    keywordColor: Color,
    stringColor: Color,
    commentColor: Color
): List<TokenSpan> {
    val spans = mutableListOf<TokenSpan>()
    var index = 0

    while (index < code.length) {
        val lineCommentStart = spec.lineCommentStarts.firstOrNull { marker ->
            code.startsWith(marker, index)
        }
        if (lineCommentStart != null) {
            val end = code.indexOf('\n', startIndex = index).let { if (it < 0) code.length else it }
            spans += TokenSpan(index, end, SpanStyle(color = commentColor))
            index = end
            continue
        }

        val blockComment = spec.blockCommentPairs.firstOrNull { (start, _) ->
            code.startsWith(start, index)
        }
        if (blockComment != null) {
            val (_, endMarker) = blockComment
            val end = code.indexOf(endMarker, startIndex = index + blockComment.first.length)
            val stop = if (end < 0) code.length else end + endMarker.length
            spans += TokenSpan(index, stop, SpanStyle(color = commentColor))
            index = stop
            continue
        }

        if (code[index] == '"' || code[index] == '\'' || code[index] == '`') {
            val end = consumeStringLiteral(code, index)
            spans += TokenSpan(index, end, SpanStyle(color = stringColor))
            index = end
            continue
        }

        if (code[index].isIdentifierStart()) {
            val end = consumeIdentifier(code, index)
            val identifier = code.substring(index, end)
            if (identifier.lowercase() in keywords) {
                spans += TokenSpan(index, end, SpanStyle(color = keywordColor))
            }
            index = end
            continue
        }

        index += 1
    }

    return spans
}

private fun lexDiffHeaders(
    code: String,
    keywordColor: Color,
    commentColor: Color
): List<TokenSpan> {
    val spans = mutableListOf<TokenSpan>()
    var offset = 0
    code.split('\n').forEach { line ->
        val end = offset + line.length
        when {
            line.startsWith("@@") || line.startsWith("+++") || line.startsWith("---") -> {
                spans += TokenSpan(offset, end, SpanStyle(color = keywordColor))
            }

            line.startsWith("+") || line.startsWith("-") -> {
                spans += TokenSpan(offset, end, SpanStyle(color = commentColor))
            }
        }
        offset = end + 1
    }
    return spans
}

private fun consumeIdentifier(text: String, start: Int): Int {
    var index = start
    while (index < text.length && text[index].isIdentifierPart()) {
        index++
    }
    return index
}

private fun consumeStringLiteral(text: String, start: Int): Int {
    val quote = text[start]
    var index = start + 1
    var escaped = false
    while (index < text.length) {
        val current = text[index]
        if (escaped) {
            escaped = false
            index++
            continue
        }
        if (current == '\\') {
            escaped = true
            index++
            continue
        }
        index++
        if (current == quote) {
            break
        }
    }
    return index.coerceAtMost(text.length)
}

private fun Char.isIdentifierStart(): Boolean = this == '_' || this.isLetter()

private fun Char.isIdentifierPart(): Boolean = this == '_' || this.isLetterOrDigit()

