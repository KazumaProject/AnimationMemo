package com.kazumaproject.animationswipememo.domain.model

import java.util.UUID

data class MemoBlock(
    val id: String,
    val text: String,
    val normalizedX: Float,
    val normalizedY: Float,
    val widthFraction: Float,
    val animationStyle: AnimationStyle,
    val textStyle: TextStyleSetting
) {
    companion object {
        fun create(
            defaultAnimation: AnimationStyle,
            x: Float = 0.5f,
            y: Float = 0.35f
        ): MemoBlock {
            return MemoBlock(
                id = UUID.randomUUID().toString(),
                text = "",
                normalizedX = x,
                normalizedY = y,
                widthFraction = 0.52f,
                animationStyle = defaultAnimation,
                textStyle = TextStyleSetting()
            )
        }
    }
}
