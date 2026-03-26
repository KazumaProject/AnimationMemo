package com.kazumaproject.animationswipememo.ui.components.render

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderUtilsTest {
    @Test
    fun renderLatex_replacesCommonCommands() {
        val rendered = renderLatex("\\alpha^2 + \\beta_1")
        assertTrue(rendered.text.contains("α"))
        assertTrue(rendered.text.contains("β"))
    }

    @Test
    fun renderLatex_supportsMinimumMathCommandsAndEnvironments() {
        val rendered = renderLatex("$$\\frac{a_1}{\\sqrt{b}} + \\sum_{i=1}^{n} i \\to \\infty$$")
        assertTrue(rendered.text.contains("(a"))
        assertTrue(rendered.text.contains("√(b)"))
        assertTrue(rendered.text.contains("∑"))
        assertTrue(rendered.text.contains("→"))
        assertTrue(rendered.text.contains("∞"))

        val aligned = renderLatex("\\begin{aligned}x&=1\\\\y&=2\\end{aligned}")
        assertTrue(aligned.text.contains("x | =1") || aligned.text.contains("x|=1"))
        assertTrue(aligned.text.contains("\n"))

        val cases = renderLatex("\\begin{cases}x^2 & x>0\\\\0 & otherwise\\end{cases}")
        assertTrue(cases.text.contains("{ "))
    }

    @Test
    fun highlightCode_marksKeywordsAndStrings() {
        val rendered = highlightCode(
            language = "kotlin",
            code = "fun main() { val msg = \"hi\" }",
            defaultColor = Color.Black
        )
        assertTrue(rendered.spanStyles.isNotEmpty())
    }

    @Test
    fun highlightCode_supportsLanguageAliasesAndPlainText() {
        val tsx = highlightCode(
            language = "TSX",
            code = "const App = () => <div>{\"ok\"}</div>",
            defaultColor = Color.Black
        )
        assertTrue(tsx.spanStyles.isNotEmpty())

        val shell = highlightCode(
            language = "Bash",
            code = "# comment\nif [ -f x ]; then echo hi; fi",
            defaultColor = Color.Black
        )
        assertTrue(shell.spanStyles.isNotEmpty())

        val plain = highlightCode(
            language = "Plain Text",
            code = "just words",
            defaultColor = Color.Black
        )
        assertTrue(plain.spanStyles.isEmpty())
        val supported = supportedCodeLanguages()
        assertFalse(supported.isEmpty())
        assertTrue(supported.contains("TypeScript"))
        assertTrue(supported.contains("TSX"))
        assertTrue(supported.contains("Kotlin"))
        assertTrue(supported.contains("PowerShell"))
        assertTrue(supported.contains("diff / patch"))
    }
}

