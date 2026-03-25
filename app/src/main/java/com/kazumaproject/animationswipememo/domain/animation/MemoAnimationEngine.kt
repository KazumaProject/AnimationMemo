package com.kazumaproject.animationswipememo.domain.animation

import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sin

object MemoAnimationEngine {
    fun frameAt(
        animationStyle: AnimationStyle,
        text: String,
        progress: Float
    ): MemoAnimationFrame {
        val normalized = progress.mod(1f)
        val pulse = ((sin((normalized * 2f * PI) - (PI / 2f)) + 1f) / 2f).toFloat()
        val wave = sin(normalized * 2f * PI).toFloat()

        return when (animationStyle) {
            AnimationStyle.None -> MemoAnimationFrame()
            AnimationStyle.Fade -> MemoAnimationFrame(
                alpha = 0.35f + (pulse * 0.65f)
            )

            AnimationStyle.Typewriter -> {
                val revealProgress = (normalized / 0.78f).coerceIn(0f, 1f)
                val visibleCharacters = ceil(text.length * revealProgress).toInt()
                MemoAnimationFrame(
                    alpha = 1f,
                    visibleCharacters = visibleCharacters
                )
            }

            AnimationStyle.Float -> MemoAnimationFrame(
                alpha = 0.98f,
                offsetXPx = wave * 6f,
                offsetYPx = -wave * 14f
            )

            AnimationStyle.Shake -> {
                val tremor = sin(normalized * 12f * PI).toFloat()
                MemoAnimationFrame(
                    offsetXPx = tremor * 10f,
                    rotationDeg = tremor * 2.8f
                )
            }

            AnimationStyle.Bounce -> {
                val bounce = abs(sin(normalized * 2f * PI)).toFloat()
                MemoAnimationFrame(
                    offsetYPx = -bounce * 22f,
                    scale = 1f + (bounce * 0.04f)
                )
            }

            AnimationStyle.Glow -> MemoAnimationFrame(
                alpha = 1f,
                glowRadiusPx = 6f + (pulse * 18f)
            )
        }
    }
}
