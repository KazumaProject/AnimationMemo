package com.kazumaproject.animationswipememo.data.local

import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.MemoTextAlign
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting
import org.json.JSONArray
import org.json.JSONObject

fun MemoEntity.toDomain(): MemoDraft {
    return MemoDraft(
        id = id,
        paperStyle = PaperStyle.valueOf(paperStyle),
        blocks = decodeBlocks(blocksJson),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun MemoDraft.toEntity(): MemoEntity {
    return MemoEntity(
        id = id,
        paperStyle = paperStyle.name,
        blocksJson = encodeBlocks(blocks),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun encodeBlocks(blocks: List<MemoBlock>): String {
    val array = JSONArray()
    blocks.forEach { block ->
        array.put(
            JSONObject().apply {
                put("id", block.id)
                put("text", block.text)
                put("x", block.normalizedX.toDouble())
                put("y", block.normalizedY.toDouble())
                put("width", block.widthFraction.toDouble())
                put("animation", block.animationStyle.name)
                put("fontSize", block.textStyle.fontSize.toDouble())
                put("textColor", block.textStyle.textColor)
                put("textAlign", block.textStyle.textAlign.name)
            }
        )
    }
    return array.toString()
}

private fun decodeBlocks(json: String): List<MemoBlock> {
    return runCatching {
        val array = JSONArray(json)
        buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(
                    MemoBlock(
                        id = item.getString("id"),
                        text = item.optString("text"),
                        normalizedX = item.optDouble("x", 0.5).toFloat(),
                        normalizedY = item.optDouble("y", 0.35).toFloat(),
                        widthFraction = item.optDouble("width", 0.52).toFloat(),
                        animationStyle = AnimationStyle.valueOf(
                            item.optString("animation", AnimationStyle.Fade.name)
                        ),
                        textStyle = TextStyleSetting(
                            fontSize = item.optDouble("fontSize", 28.0).toFloat(),
                            textColor = item.optInt("textColor", 0xFF2D241C.toInt()),
                            textAlign = MemoTextAlign.valueOf(
                                item.optString("textAlign", MemoTextAlign.Center.name)
                            )
                        )
                    )
                )
            }
        }
    }.getOrElse {
        listOf(MemoBlock.create(defaultAnimation = AnimationStyle.Fade))
    }
}
