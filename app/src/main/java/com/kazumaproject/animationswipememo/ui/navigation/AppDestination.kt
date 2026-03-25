package com.kazumaproject.animationswipememo.ui.navigation

sealed class AppDestination(
    val route: String,
    val baseRoute: String,
    val label: String
) {
    data object Editor : AppDestination(
        route = "editor?memoId={memoId}",
        baseRoute = "editor",
        label = "Editor"
    ) {
        fun createRoute(memoId: String? = null): String {
            return if (memoId.isNullOrEmpty()) {
                baseRoute
            } else {
                "editor?memoId=$memoId"
            }
        }
    }

    data object MemoList : AppDestination(
        route = "memo_list",
        baseRoute = "memo_list",
        label = "Memos"
    )

    data object Settings : AppDestination(
        route = "settings",
        baseRoute = "settings",
        label = "Settings"
    )
}
