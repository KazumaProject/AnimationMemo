package com.kazumaproject.animationswipememo.ui.components.render

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em

private val latexCommandMap = mapOf(
    "\\alpha" to "α",
    "\\beta" to "β",
    "\\gamma" to "γ",
    "\\delta" to "δ",
    "\\theta" to "θ",
    "\\lambda" to "λ",
    "\\mu" to "μ",
    "\\pi" to "π",
    "\\sigma" to "σ",
    "\\omega" to "ω",
    "\\times" to "×",
    "\\cdot" to "·",
    "\\pm" to "±",
    "\\leq" to "≤",
    "\\geq" to "≥",
    "\\neq" to "≠",
    "\\infty" to "∞",
    "\\sum" to "∑",
    "\\int" to "∫",
    "\\to" to "→",
    "\\rightarrow" to "→",
    "\\Rightarrow" to "⇒",
    "\\mapsto" to "↦",
    "\\lim" to "lim",
    "\\partial" to "∂",
    "\\nabla" to "∇"
    ,"\\prod" to "∏"
    ,"\\forall" to "∀"
    ,"\\exists" to "∃"
    ,"\\approx" to "≈"
    ,"\\neq" to "≠"
    ,"\\subset" to "⊂"
    ,"\\subseteq" to "⊆"
    ,"\\supset" to "⊃"
    ,"\\supseteq" to "⊇"
)

private val superStyle = SpanStyle(
    baselineShift = BaselineShift.Superscript,
    fontSize = 0.78.em,
    fontFamily = FontFamily.Monospace
)

private val subStyle = SpanStyle(
    baselineShift = BaselineShift.Subscript,
    fontSize = 0.78.em,
    fontFamily = FontFamily.Monospace
)

fun renderLatex(expression: String): AnnotatedString {
    val normalized = normalizeLatex(expression)

    return buildAnnotatedString {
        var i = 0
        while (i < normalized.length) {
            when {
                normalized.startsWith(" frac ", i) -> {
                    append("(")
                    i += " frac ".length
                }

                normalized.startsWith(" sqrt ", i) -> {
                    append("√(")
                    i += " sqrt ".length
                }

                normalized[i] == '^' || normalized[i] == '_' -> {
                    val style = if (normalized[i] == '^') superStyle else subStyle
                    i += 1
                    val token = if (i < normalized.length && normalized[i] == '{') {
                        val end = normalized.indexOf('}', startIndex = i + 1).takeIf { it >= 0 }
                        if (end != null) {
                            val value = normalized.substring(i + 1, end)
                            i = end + 1
                            value
                        } else {
                            val value = normalized.substring(i)
                            i = normalized.length
                            value
                        }
                    } else {
                        val value = normalized.getOrNull(i)?.toString().orEmpty()
                        i += if (value.isEmpty()) 0 else 1
                        value
                    }
                    pushStyle(style)
                    append(token)
                    pop()
                }

                else -> {
                    append(normalized[i])
                    i += 1
                }
            }
        }

    }
}

fun latexToPlainText(expression: String): String {
    return renderLatex(expression).text
}

private fun normalizeLatex(input: String): String {
    var text = unwrapEmbeddedMathSegments(input.trim())
    text = stripMathDelimiters(text)
    text = replaceEnvironments(text)
    text = replaceCommandWrappers(text, "\\\\text")
    text = replaceCommandWrappers(text, "\\\\operatorname")
    text = replaceCommandWrappers(text, "\\\\mathbb")
    text = replaceCommandWrappers(text, "\\\\mathbf")
    text = replaceCommandWrappers(text, "\\\\mathrm")
    text = replaceCommandWrappers(text, "\\\\mathcal")
    text = text.replace("\\left", "").replace("\\right", "")
    text = replaceFracAndSqrt(text)
    text = latexCommandMap.entries.fold(text) { acc, (latex, glyph) ->
        acc.replace(latex, glyph)
    }
    text = text
        .replace("\\\\", "\n")
        .replace('&', '|')
        .replace(Regex("\\s+\\|\\s+"), " | ")
        .replace(Regex("[ \t]+"), " ")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
    return text
}

private fun unwrapEmbeddedMathSegments(input: String): String {
    var text = input
    val patterns = listOf(
        Regex("\\\\\\((.*?)\\\\\\)", RegexOption.DOT_MATCHES_ALL),
        Regex("\\\\\\[(.*?)\\\\\\]", RegexOption.DOT_MATCHES_ALL),
        Regex("\\$\\$(.*?)\\$\\$", RegexOption.DOT_MATCHES_ALL),
        Regex("\\$(.*?)\\$", RegexOption.DOT_MATCHES_ALL)
    )
    patterns.forEach { pattern ->
        while (true) {
            val match = pattern.find(text) ?: break
            val inner = match.groupValues.getOrNull(1).orEmpty()
            text = text.replaceRange(match.range, inner)
        }
    }
    return text
}

private fun stripMathDelimiters(input: String): String {
    return when {
        input.startsWith("$$") && input.endsWith("$$") && input.length >= 4 -> input.drop(2).dropLast(2)
        input.startsWith("\\[") && input.endsWith("\\]") -> input.removePrefix("\\[").removeSuffix("\\]")
        input.startsWith("\\(") && input.endsWith("\\)") -> input.removePrefix("\\(").removeSuffix("\\)")
        input.startsWith("$") && input.endsWith("$") && input.length >= 2 -> input.drop(1).dropLast(1)
        else -> input
    }
}

private fun replaceCommandWrappers(text: String, commandRegex: String): String {
    val regex = Regex("$commandRegex\\{([^{}]*)}")
    var current = text
    while (true) {
        val next = regex.replace(current) { it.groupValues[1] }
        if (next == current) return next
        current = next
    }
}

private fun replaceEnvironments(text: String): String {
    val environmentRegex = Regex("\\\\begin\\{(aligned|align|gather|cases|matrix|pmatrix|bmatrix)\\}(.*?)\\\\end\\{\\1\\}", RegexOption.DOT_MATCHES_ALL)
    var current = text
    while (true) {
        val match = environmentRegex.find(current) ?: return current
        val env = match.groupValues[1]
        val body = match.groupValues[2]
        val lines = body.split("\\\\")
            .map { line -> line.trim().replace('&', '|') }
            .filter { it.isNotBlank() }

        val replacement = when (env) {
            "cases" -> lines.joinToString(separator = "\n") { "{ $it" }
            "matrix", "pmatrix", "bmatrix" -> lines.joinToString(separator = "\n") { "[ $it ]" }
            else -> lines.joinToString(separator = "\n")
        }
        current = current.replaceRange(match.range, replacement)
    }
}

private fun replaceFracAndSqrt(text: String): String {
    var current = text
    var cursor = 0
    while (cursor < current.length) {
        val fracIndex = current.indexOf("\\frac", startIndex = cursor)
        val sqrtIndex = current.indexOf("\\sqrt", startIndex = cursor)
        val commandIndex = listOf(fracIndex, sqrtIndex).filter { it >= 0 }.minOrNull() ?: break

        if (current.startsWith("\\frac", commandIndex)) {
            val numerator = readBraced(current, commandIndex + "\\frac".length)
            if (numerator == null) {
                cursor = commandIndex + 1
                continue
            }
            val denominator = readBraced(current, numerator.second)
            if (denominator == null) {
                cursor = commandIndex + 1
                continue
            }
            val replacement = "(${normalizeLatex(numerator.first)})/(${normalizeLatex(denominator.first)})"
            current = current.replaceRange(commandIndex, denominator.second, replacement)
            cursor = commandIndex + replacement.length
            continue
        }

        if (current.startsWith("\\sqrt", commandIndex)) {
            val radicand = readBraced(current, commandIndex + "\\sqrt".length)
            if (radicand == null) {
                cursor = commandIndex + 1
                continue
            }
            val replacement = "√(${normalizeLatex(radicand.first)})"
            current = current.replaceRange(commandIndex, radicand.second, replacement)
            cursor = commandIndex + replacement.length
            continue
        }
    }
    return current
}

private fun readBraced(text: String, startIndex: Int): Pair<String, Int>? {
    var index = startIndex
    while (index < text.length && text[index].isWhitespace()) {
        index++
    }
    if (index >= text.length || text[index] != '{') return null

    val contentStart = index + 1
    var depth = 1
    var cursor = contentStart
    while (cursor < text.length && depth > 0) {
        when (text[cursor]) {
            '{' -> depth++
            '}' -> depth--
        }
        cursor++
    }
    if (depth != 0) return null
    return text.substring(contentStart, cursor - 1) to cursor
}

