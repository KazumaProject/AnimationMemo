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

            AnimationStyle.Pulse -> MemoAnimationFrame(
                alpha = 0.8f + (pulse * 0.2f),
                scale = 0.94f + (pulse * 0.12f)
            )

            AnimationStyle.Zoom -> MemoAnimationFrame(
                scale = 1f + (pulse * 0.18f),
                alpha = 0.92f + (pulse * 0.08f)
            )

            AnimationStyle.Wiggle -> MemoAnimationFrame(
                rotationDeg = wave * 5.5f,
                offsetXPx = wave * 4f
            )

            AnimationStyle.Flicker -> MemoAnimationFrame(
                alpha = if ((normalized * 12).toInt() % 3 == 0) 0.38f else 1f
            )

            AnimationStyle.Stamp -> {
                val stamp = if (normalized < 0.25f) {
                    normalized / 0.25f
                } else {
                    1f - ((normalized - 0.25f) / 0.75f * 0.25f)
                }.coerceIn(0.78f, 1.12f)
                MemoAnimationFrame(
                    scale = stamp,
                    rotationDeg = (1f - stamp) * -8f,
                    alpha = 0.78f + (pulse * 0.22f)
                )
            }

            AnimationStyle.Slide -> MemoAnimationFrame(
                alpha = 0.94f,
                offsetXPx = wave * 18f,
                rotationDeg = wave * 1.4f
            )

            AnimationStyle.Spin -> MemoAnimationFrame(
                alpha = 0.96f,
                rotationDeg = normalized * 360f,
                scale = 0.96f + (pulse * 0.08f)
            )

            AnimationStyle.Wave -> MemoAnimationFrame(
                alpha = 0.96f,
                offsetYPx = sin(normalized * 4f * PI).toFloat() * 12f,
                rotationDeg = wave * 3.5f
            )

            AnimationStyle.Pop -> {
                val pop = if (normalized < 0.2f) {
                    0.82f + ((normalized / 0.2f) * 0.34f)
                } else {
                    1.16f - (((normalized - 0.2f) / 0.8f) * 0.16f)
                }.coerceIn(0.82f, 1.16f)
                MemoAnimationFrame(
                    alpha = 0.88f + (pulse * 0.12f),
                    scale = pop
                )
            }
        }
    }
}
