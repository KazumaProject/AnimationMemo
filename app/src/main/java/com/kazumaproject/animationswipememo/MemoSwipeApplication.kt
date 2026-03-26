package com.kazumaproject.animationswipememo

import android.app.Application
import com.kazumaproject.animationswipememo.di.AppContainer
import com.kazumaproject.animationswipememo.di.DefaultAppContainer
import com.kazumaproject.animationswipememo.platform.AppContextHolder

class MemoSwipeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)
        container = DefaultAppContainer(this)
    }
}
