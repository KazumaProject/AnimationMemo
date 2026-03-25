package com.kazumaproject.animationswipememo.domain.export

import android.net.Uri
import com.kazumaproject.animationswipememo.domain.model.GifQuality
import com.kazumaproject.animationswipememo.domain.model.MemoDraft

data class ExportRequest(
    val memo: MemoDraft,
    val quality: GifQuality,
    val darkTheme: Boolean
)

data class ExportResult(
    val uri: Uri,
    val displayName: String
)

interface AnimationExporter {
    suspend fun exportGif(request: ExportRequest): ExportResult
    suspend fun exportPng(request: ExportRequest): ExportResult
}
