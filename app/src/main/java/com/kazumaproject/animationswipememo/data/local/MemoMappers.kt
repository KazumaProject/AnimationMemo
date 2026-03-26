package com.kazumaproject.animationswipememo.data.local

import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.MemoBlock
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.model.MemoFontFamily
import com.kazumaproject.animationswipememo.domain.model.ListAppearance
import com.kazumaproject.animationswipememo.domain.model.ListItem
import com.kazumaproject.animationswipememo.domain.model.ListItemAppearance
import com.kazumaproject.animationswipememo.domain.model.ListItemType
import com.kazumaproject.animationswipememo.domain.model.MemoTextAlign
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import com.kazumaproject.animationswipememo.domain.model.SavedDrawing
import com.kazumaproject.animationswipememo.domain.model.StrokeData
import com.kazumaproject.animationswipememo.domain.model.StrokePoint
import com.kazumaproject.animationswipememo.domain.model.TextStyleSetting
import org.json.JSONArray
import org.json.JSONObject

fun MemoEntity.toDomain(): MemoDraft {
    return MemoDraft(
        id = id,
        title = title,
        paperStyle = PaperStyle.fromName(paperStyle),
        blocks = decodeBlocks(blocksJson),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun MemoDraft.toEntity(): MemoEntity {
    return MemoEntity(
        id = id,
        title = title,
        paperStyle = paperStyle.name,
        blocksJson = encodeBlocks(blocks),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SavedDrawingEntity.toDomain(): SavedDrawing {
    return SavedDrawing(
        id = id,
        name = name,
        strokes = decodeStrokes(strokesJson),
        widthFraction = widthFraction,
        heightFraction = heightFraction,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SavedDrawing.toEntity(): SavedDrawingEntity {
    return SavedDrawingEntity(
        id = id,
        name = name,
        strokesJson = encodeStrokes(strokes),
        widthFraction = widthFraction,
        heightFraction = heightFraction,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun encodeBlocks(blocks: List<MemoBlock>): String {
    val array = JSONArray()
    blocks.forEach { block ->
        val strokesJson = JSONArray(encodeStrokes(block.strokes))
        val listItemsJson = JSONArray().apply {
            block.listItems.forEach { item ->
                put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("text", item.text)
                        put("indentLevel", item.indentLevel)
                        put("itemType", item.itemType.name)
                        put("checked", item.checked)
                        put("isExpanded", item.isExpanded)
                        put("fontScaleOverride", item.appearanceOverride?.fontScaleOverride)
                    }
                )
            }
        }
        array.put(
            JSONObject().apply {
                put("id", block.id)
                put("type", block.type.name)
                put("text", block.text)
                put("x", block.normalizedX.toDouble())
                put("y", block.normalizedY.toDouble())
                put("width", block.widthFraction.toDouble())
                put("height", block.heightFraction.toDouble())
                put("contentAspectRatio", block.contentAspectRatio?.toDouble())
                put("animation", block.animationStyle.name)
                put("fontSize", block.textStyle.fontSize.toDouble())
                put("textColor", block.textStyle.textColor)
                put("textAlign", block.textStyle.textAlign.name)
                put("fontFamily", block.textStyle.fontFamily.name)
                put("isBold", block.textStyle.isBold)
                put("isItalic", block.textStyle.isItalic)
                put("isUnderline", block.textStyle.isUnderline)
                put("imageUri", block.imageUri)
                put("strokes", strokesJson)
                put("listItems", listItemsJson)
                put("listFontScale", block.listAppearance?.fontScale?.toDouble())
                put("listLevelScaleStep", block.listAppearance?.levelScaleStep?.toDouble())
                put("listMinFontScale", block.listAppearance?.minFontScale?.toDouble())
                put("listIndentStepDp", block.listAppearance?.indentStepDp?.toDouble())
                put("listMarkerGapDp", block.listAppearance?.markerGapDp?.toDouble())
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
                val type = MemoBlockType.valueOf(
                    item.optString("type", MemoBlockType.Text.name)
                )
                val strokes = decodeStrokes(
                    (item.optJSONArray("strokes") ?: JSONArray()).toString()
                )
                val legacyListItemType = when (item.optString("listStyle", "").uppercase()) {
                    "ORDERED" -> ListItemType.ORDERED
                    "CHECKBOX" -> ListItemType.CHECKBOX
                    "UNORDERED" -> ListItemType.UNORDERED
                    else -> ListItemType.UNORDERED
                }
                add(
                    MemoBlock(
                        id = item.getString("id"),
                        type = type,
                        text = item.optString("text"),
                        normalizedX = item.optDouble("x", 0.5).toFloat(),
                        normalizedY = item.optDouble("y", 0.35).toFloat(),
                        widthFraction = item.optDouble("width", 0.52).toFloat(),
                        heightFraction = item.optDouble("height", 0.12).toFloat(),
                        contentAspectRatio = item.optDouble(
                            "contentAspectRatio",
                            item.optDouble("width", 0.52) / item.optDouble("height", 0.12)
                        ).toFloat(),
                        animationStyle = AnimationStyle.valueOf(
                            item.optString("animation", AnimationStyle.Fade.name)
                        ),
                        textStyle = TextStyleSetting(
                            fontSize = item.optDouble("fontSize", 28.0).toFloat(),
                            textColor = item.optInt("textColor", 0xFF2D241C.toInt()),
                            textAlign = MemoTextAlign.valueOf(
                                item.optString("textAlign", MemoTextAlign.Center.name)
                            ),
                            fontFamily = MemoFontFamily.valueOf(
                                item.optString("fontFamily", MemoFontFamily.SystemSerif.name)
                            ),
                            isBold = item.optBoolean("isBold", false),
                            isItalic = item.optBoolean("isItalic", false),
                            isUnderline = item.optBoolean("isUnderline", false)
                        ),
                        imageUri = item.optString("imageUri").ifBlank { null },
                        strokes = strokes,
                        listItems = (item.optJSONArray("listItems") ?: JSONArray()).let { itemsJson ->
                            buildList {
                                repeat(itemsJson.length()) { listItemIndex ->
                                    val listItemJson = itemsJson.getJSONObject(listItemIndex)
                                    add(
                                        ListItem(
                                            id = listItemJson.optString("id").ifBlank {
                                                java.util.UUID.randomUUID().toString()
                                            },
                                            text = listItemJson.optString("text"),
                                            indentLevel = listItemJson.optInt("indentLevel", 0).coerceAtLeast(0),
                                            itemType = listItemJson.optString("itemType")
                                                .takeIf { it.isNotBlank() }
                                                ?.let { runCatching { ListItemType.valueOf(it) }.getOrNull() }
                                                ?: legacyListItemType,
                                            checked = listItemJson.optBoolean("checked", false),
                                            isExpanded = if (listItemJson.has("isExpanded")) {
                                                listItemJson.optBoolean("isExpanded", true)
                                            } else {
                                                !listItemJson.optBoolean("collapsed", false)
                                            },
                                            appearanceOverride = listItemJson.optDouble("fontScaleOverride", Double.NaN)
                                                .takeIf { it.isFinite() }
                                                ?.toFloat()
                                                ?.let { scale -> ListItemAppearance(fontScaleOverride = scale) }
                                        )
                                    )
                                }
                            }
                        },
                        listAppearance = item.optJSONArray("listItems")?.let {
                            ListAppearance(
                                fontScale = item.optDouble("listFontScale", 1.0).toFloat(),
                                levelScaleStep = item.optDouble("listLevelScaleStep", 0.04).toFloat(),
                                minFontScale = item.optDouble("listMinFontScale", 0.72).toFloat(),
                                indentStepDp = item.optDouble("listIndentStepDp", 16.0).toFloat(),
                                markerGapDp = item.optDouble("listMarkerGapDp", 8.0).toFloat()
                            )
                        }
                    )
                )
            }
        }
    }.getOrElse {
        listOf(MemoBlock.createText(defaultAnimation = AnimationStyle.Fade))
    }
}

private fun encodeStrokes(strokes: List<StrokeData>): String {
    val strokesJson = JSONArray()
    strokes.forEach { stroke ->
        val pointsJson = JSONArray()
        stroke.points.forEach { point ->
            pointsJson.put(
                JSONObject().apply {
                    put("x", point.x.toDouble())
                    put("y", point.y.toDouble())
                }
            )
        }
        strokesJson.put(
            JSONObject().apply {
                put("color", stroke.color)
                put("width", stroke.width.toDouble())
                put("points", pointsJson)
            }
        )
    }
    return strokesJson.toString()
}

private fun decodeStrokes(json: String): List<StrokeData> {
    return runCatching {
        val strokesJson = JSONArray(json)
        buildList {
            repeat(strokesJson.length()) { strokeIndex ->
                val strokeJson = strokesJson.getJSONObject(strokeIndex)
                val pointsJson = strokeJson.optJSONArray("points") ?: JSONArray()
                val points = buildList {
                    repeat(pointsJson.length()) { pointIndex ->
                        val pointJson = pointsJson.getJSONObject(pointIndex)
                        add(
                            StrokePoint(
                                x = pointJson.optDouble("x", 0.0).toFloat(),
                                y = pointJson.optDouble("y", 0.0).toFloat()
                            )
                        )
                    }
                }
                add(
                    StrokeData(
                        points = points,
                        color = strokeJson.optInt("color", TextStyleSetting.DEFAULT_LIGHT_TEXT_COLOR),
                        width = strokeJson.optDouble("width", 4.0).toFloat()
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
