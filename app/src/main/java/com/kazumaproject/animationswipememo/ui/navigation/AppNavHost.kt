package com.kazumaproject.animationswipememo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kazumaproject.animationswipememo.MemoSwipeApplication
import com.kazumaproject.animationswipememo.ui.editor.EditorScreen
import com.kazumaproject.animationswipememo.ui.editor.EditorViewModel
import com.kazumaproject.animationswipememo.ui.list.MemoListScreen
import com.kazumaproject.animationswipememo.ui.list.MemoListViewModel
import com.kazumaproject.animationswipememo.ui.settings.SettingsScreen
import com.kazumaproject.animationswipememo.ui.settings.SettingsViewModel

@Composable
fun AppNavHost(
    navController: androidx.navigation.NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as MemoSwipeApplication
    val container = application.container

    NavHost(
        navController = navController,
        startDestination = AppDestination.Editor.baseRoute,
        modifier = modifier
    ) {
        composable(
            route = AppDestination.Editor.route,
            arguments = listOf(
                navArgument("memoId") {
                    nullable = true
                    defaultValue = null
                    type = NavType.StringType
                }
            )
        ) {
            val viewModel: EditorViewModel = viewModel(
                factory = EditorViewModel.factory(container)
            )
            EditorScreen(
                viewModel = viewModel,
                onOpenList = {
                    navController.navigate(AppDestination.MemoList.route)
                },
                onOpenSettings = {
                    navController.navigate(AppDestination.Settings.route)
                }
            )
        }

        composable(route = AppDestination.MemoList.route) {
            val viewModel: MemoListViewModel = viewModel(
                factory = MemoListViewModel.factory(container)
            )
            MemoListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onMemoClick = { memoId ->
                    navController.navigate(AppDestination.Editor.createRoute(memoId))
                }
            )
        }

        composable(route = AppDestination.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(container)
            )
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
