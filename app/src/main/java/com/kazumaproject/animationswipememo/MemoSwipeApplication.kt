package com.kazumaproject.animationswipememo

import android.app.Application
import com.kazumaproject.animationswipememo.di.AppContainer
import com.kazumaproject.animationswipememo.di.DefaultAppContainer

class MemoSwipeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
