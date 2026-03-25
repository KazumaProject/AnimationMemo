package com.kazumaproject.animationswipememo.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.kazumaproject.animationswipememo.domain.export.AnimationExporter
import com.kazumaproject.animationswipememo.domain.export.ExportRequest
import com.kazumaproject.animationswipememo.domain.export.ExportResult
import com.squareup.gifencoder.FloydSteinbergDitherer
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import com.squareup.gifencoder.KMeansQuantizer
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GifMemoAnimationExporter(
    private val context: Context,
    private val frameRenderer: MemoBitmapFrameRenderer
) : AnimationExporter {
    override suspend fun exportGif(request: ExportRequest): ExportResult = withContext(Dispatchers.IO) {
        val displayName = buildDisplayName(extension = "gif")
        val output = createOutput(displayName, mimeType = "image/gif")
        try {
            output.outputStream.use { stream ->
                writeGif(stream, request)
            }
            output.markComplete()
            ExportResult(
                uri = output.uri,
                displayName = displayName
            )
        } catch (throwable: Throwable) {
            output.cleanup()
            throw throwable
        }
    }

    override suspend fun exportPng(request: ExportRequest): ExportResult = withContext(Dispatchers.IO) {
        val displayName = buildDisplayName(extension = "png")
        val output = createOutput(displayName, mimeType = "image/png")
        try {
            output.outputStream.use { stream ->
                val bitmap = frameRenderer.renderFrame(
                    memo = request.memo,
                    width = request.quality.width,
                    height = request.quality.height,
                    progress = 0f,
                    darkTheme = request.darkTheme
                )
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                bitmap.recycle()
            }
            output.markComplete()
            ExportResult(uri = output.uri, displayName = displayName)
        } catch (throwable: Throwable) {
            output.cleanup()
            throw throwable
        }
    }

    private fun writeGif(
        outputStream: OutputStream,
        request: ExportRequest
    ) {
        val quality = request.quality
        val frameCount = ((quality.durationMillis / 1000f) * quality.fps).toInt().coerceAtLeast(1)
        val frameDelayMillis = (1000L / quality.fps.toLong()).coerceAtLeast(1L)
        val imageOptions = ImageOptions().apply {
            setDelay(frameDelayMillis, TimeUnit.MILLISECONDS)
            setDitherer(FloydSteinbergDitherer.INSTANCE)
            setColorQuantizer(KMeansQuantizer.INSTANCE)
        }

        val encoder = GifEncoder(outputStream, quality.width, quality.height, 0)
        repeat(frameCount) { index ->
            val progress = index.toFloat() / frameCount.toFloat()
            val bitmap = frameRenderer.renderFrame(
                memo = request.memo,
                width = quality.width,
                height = quality.height,
                progress = progress,
                darkTheme = request.darkTheme
            )
            encoder.addImage(bitmap.toPixelMatrix(), imageOptions)
            bitmap.recycle()
        }
        encoder.finishEncoding()
    }

    private fun buildDisplayName(extension: String): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "memo_${formatter.format(Date())}.$extension"
    }

    private fun createOutput(displayName: String, mimeType: String): OutputTarget {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createMediaStoreOutput(displayName, mimeType)
        } else {
            createFileProviderOutput(displayName)
        }
    }

    private fun createMediaStoreOutput(displayName: String, mimeType: String): OutputTarget {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AnimationSwipeMemo")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        )

        return OutputTarget(
            uri = uri,
            outputStream = requireNotNull(resolver.openOutputStream(uri)),
            markComplete = {
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, updateValues, null, null)
            },
            cleanup = {
                resolver.delete(uri, null, null)
            }
        )
    }

    private fun createFileProviderOutput(displayName: String): OutputTarget {
        val directory = File(
            requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)),
            "AnimationSwipeMemo"
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, displayName)
        if (!file.exists()) {
            file.createNewFile()
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return OutputTarget(
            uri = uri,
            outputStream = file.outputStream(),
            markComplete = {},
            cleanup = {
                file.delete()
            }
        )
    }

    private fun android.graphics.Bitmap.toPixelMatrix(): Array<IntArray> {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return Array(width) { x ->
            IntArray(height) { y ->
                pixels[(y * width) + x]
            }
        }
    }

    private data class OutputTarget(
        val uri: Uri,
        val outputStream: OutputStream,
        val markComplete: () -> Unit,
        val cleanup: () -> Unit
    )
}
