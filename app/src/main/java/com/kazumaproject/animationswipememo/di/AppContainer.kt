package com.kazumaproject.animationswipememo.di

import android.content.Context
import com.kazumaproject.animationswipememo.data.export.GifMemoAnimationExporter
import com.kazumaproject.animationswipememo.data.export.MemoBitmapFrameRenderer
import com.kazumaproject.animationswipememo.data.local.MemoDatabase
import com.kazumaproject.animationswipememo.data.preferences.SettingsPreferencesStorage
import com.kazumaproject.animationswipememo.data.repository.DefaultSettingsRepository
import com.kazumaproject.animationswipememo.data.repository.OfflineMemoRepository
import com.kazumaproject.animationswipememo.domain.export.AnimationExporter
import com.kazumaproject.animationswipememo.domain.repository.MemoRepository
import com.kazumaproject.animationswipememo.domain.repository.SettingsRepository

interface AppContainer {
    val memoRepository: MemoRepository
    val settingsRepository: SettingsRepository
    val animationExporter: AnimationExporter
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: MemoDatabase by lazy {
        MemoDatabase.create(context)
    }

    private val settingsStorage: SettingsPreferencesStorage by lazy {
        SettingsPreferencesStorage(context)
    }

    private val frameRenderer: MemoBitmapFrameRenderer by lazy {
        MemoBitmapFrameRenderer(context)
    }

    override val memoRepository: MemoRepository by lazy {
        OfflineMemoRepository(database.memoDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        DefaultSettingsRepository(settingsStorage)
    }

    override val animationExporter: AnimationExporter by lazy {
        GifMemoAnimationExporter(
            context = context,
            frameRenderer = frameRenderer
        )
    }
}
