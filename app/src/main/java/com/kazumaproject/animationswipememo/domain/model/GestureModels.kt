package com.kazumaproject.animationswipememo.domain.model

enum class GestureAction(val displayName: String) {
    Save("Save"),
    Discard("Discard"),
    ExportGif("Export GIF"),
    None("None")
}

enum class GestureDirection(val displayName: String) {
    SwipeLeft("SwipeLeft"),
    SwipeRight("SwipeRight"),
    PullDown("PullDown")
}

data class GestureSettings(
    val swipeLeft: GestureAction = GestureAction.ExportGif,
    val swipeRight: GestureAction = GestureAction.Save,
    val pullDown: GestureAction = GestureAction.Discard
) {
    fun actionFor(direction: GestureDirection): GestureAction {
        return when (direction) {
            GestureDirection.SwipeLeft -> swipeLeft
            GestureDirection.SwipeRight -> swipeRight
            GestureDirection.PullDown -> pullDown
        }
    }

    fun update(direction: GestureDirection, action: GestureAction): GestureSettings {
        return when (direction) {
            GestureDirection.SwipeLeft -> copy(swipeLeft = action)
            GestureDirection.SwipeRight -> copy(swipeRight = action)
            GestureDirection.PullDown -> copy(pullDown = action)
        }
    }
}
