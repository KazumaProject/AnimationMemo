package com.kazumaproject.animationswipememo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.kazumaproject.animationswipememo.ui.navigation.AppNavHost
import com.kazumaproject.animationswipememo.ui.theme.AnimationSwipeMemoTheme

@Composable
fun MemoSwipeApp() {
    val application = LocalContext.current.applicationContext as MemoSwipeApplication
    val settingsRepository = application.container.settingsRepository
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = settingsRepository.currentValue()
    )
    val navController = rememberNavController()

    AnimationSwipeMemoTheme(themeMode = settings.themeMode) {
        AppNavHost(navController = navController)
    }
}
