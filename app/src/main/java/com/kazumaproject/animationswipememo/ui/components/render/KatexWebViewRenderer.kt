package com.kazumaproject.animationswipememo.ui.components.render

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun KatexBlockView(
    expression: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val html = remember(expression, darkTheme) {
        katexHtml(expression = expression, darkTheme = darkTheme, displayMode = true)
    }
    AndroidView(
        modifier = modifier.heightIn(min = 56.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
        }
    )
}

class KatexBitmapRenderer {
    private val cache = ConcurrentHashMap<String, Bitmap>()

    @SuppressLint("SetJavaScriptEnabled")
    fun render(
        context: android.content.Context,
        expression: String,
        widthPx: Int,
        heightPx: Int,
        darkTheme: Boolean
    ): Bitmap? {
        if (expression.isBlank() || widthPx <= 0 || heightPx <= 0) return null
        val cacheKey = "$expression#$widthPx#$heightPx#$darkTheme"
        cache[cacheKey]?.let { return it.copy(it.config ?: Bitmap.Config.ARGB_8888, false) }

        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())
        var result: Bitmap? = null

        handler.post {
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webChromeClient = WebChromeClient()
            }
            val html = katexHtml(expression = expression, darkTheme = darkTheme, displayMode = true)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.postDelayed({
                        runCatching {
                            webView.measure(
                                android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY),
                                android.view.View.MeasureSpec.makeMeasureSpec(heightPx, android.view.View.MeasureSpec.EXACTLY)
                            )
                            webView.layout(0, 0, widthPx, heightPx)
                            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            webView.draw(canvas)
                            cache[cacheKey] = bitmap
                            result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
                        }
                        webView.destroy()
                        latch.countDown()
                    }, 70)
                }
            }
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
        }

        latch.await(1800, TimeUnit.MILLISECONDS)
        return result
    }
}

private fun katexHtml(expression: String, darkTheme: Boolean, displayMode: Boolean): String {
    val expressionJsLiteral = JSONObject.quote(expression)
    val textColor = if (darkTheme) "#F7F0E2" else "#2D241C"
    val bgColor = if (darkTheme) "#00000000" else "#00000000"
    val displayModeLiteral = if (displayMode) "true" else "false"
    return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
  <link rel="stylesheet" href="katex/katex.min.css" />
  <script src="katex/katex.min.js"></script>
  <style>
    html, body { margin: 0; padding: 0; background: $bgColor; color: $textColor; overflow: hidden; }
    #math { padding: 6px 8px; color: $textColor; font-size: 1.05em; line-height: 1.32; min-height: 24px; }
    .katex { color: $textColor !important; }
    .fallback { font-family: monospace; white-space: pre-wrap; }
  </style>
</head>
<body>
  <div id="math"></div>
  <script>
    (function() {
      var expr = $expressionJsLiteral;
      var container = document.getElementById('math');
      try {
        if (!window.katex) {
          container.className = 'fallback';
          container.textContent = expr;
          return;
        }
        katex.render(expr, container, {
          throwOnError: false,
          displayMode: $displayModeLiteral,
          strict: 'warn'
        });
      } catch (err) {
        container.className = 'fallback';
        container.textContent = expr;
      }
    })();
  </script>
</body>
</html>
""".trimIndent()
}
