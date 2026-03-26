package com.kazumaproject.animationswipememo.data.local

import com.kazumaproject.animationswipememo.domain.model.ConversationItem
import com.kazumaproject.animationswipememo.domain.model.ConversationRole
import com.kazumaproject.animationswipememo.domain.model.HeadingLevel
import com.kazumaproject.animationswipememo.domain.model.MemoBlockPayload
import com.kazumaproject.animationswipememo.domain.model.MemoBlockType
import com.kazumaproject.animationswipememo.domain.model.TableRow
import com.kazumaproject.animationswipememo.domain.model.ToggleChildBlock
import org.json.JSONArray
import org.json.JSONObject

internal object MemoBlockPayloadJsonCodec {
    private const val PAYLOAD_VERSION = 1

    fun encode(payload: MemoBlockPayload): JSONObject {
        return when (payload) {
            MemoBlockPayload.None -> JSONObject().apply {
                put("kind", "none")
                put("version", PAYLOAD_VERSION)
            }

            is MemoBlockPayload.Heading -> JSONObject().apply {
                put("kind", "heading")
                put("version", PAYLOAD_VERSION)
                put("level", payload.level.name)
                put("text", payload.text)
            }

            is MemoBlockPayload.Toggle -> JSONObject().apply {
                put("kind", "toggle")
                put("version", PAYLOAD_VERSION)
                put("title", payload.title)
                put("initiallyExpanded", payload.initiallyExpanded)
                put("childBlocks", JSONArray().apply {
                    payload.childBlocks.forEach { child ->
                        put(
                            JSONObject().apply {
                                put("id", child.id)
                                put("type", child.type.name)
                                put("text", child.text)
                            }
                        )
                    }
                })
            }

            is MemoBlockPayload.Quote -> JSONObject().apply {
                put("kind", "quote")
                put("version", PAYLOAD_VERSION)
                put("text", payload.text)
            }

            is MemoBlockPayload.Code -> JSONObject().apply {
                put("kind", "code")
                put("version", PAYLOAD_VERSION)
                put("language", payload.language)
                put("code", payload.code)
            }

            MemoBlockPayload.Divider -> JSONObject().apply {
                put("kind", "divider")
                put("version", PAYLOAD_VERSION)
            }

            is MemoBlockPayload.LinkCard -> JSONObject().apply {
                put("kind", "linkCard")
                put("version", PAYLOAD_VERSION)
                put("url", payload.url)
                put("title", payload.title)
                put("description", payload.description)
                put("imageUrl", payload.imageUrl)
                put("faviconUrl", payload.faviconUrl)
            }

            is MemoBlockPayload.Table -> JSONObject().apply {
                put("kind", "table")
                put("version", PAYLOAD_VERSION)
                put("rows", JSONArray().apply {
                    payload.rows.forEach { row ->
                        put(
                            JSONObject().apply {
                                put("id", row.id)
                                put("cells", JSONArray(row.cells))
                            }
                        )
                    }
                })
            }

            is MemoBlockPayload.Conversation -> JSONObject().apply {
                put("kind", "conversation")
                put("version", PAYLOAD_VERSION)
                put("items", JSONArray().apply {
                    payload.items.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("id", item.id)
                                put("speaker", item.speaker)
                                put("text", item.text)
                                put("role", item.role.name)
                            }
                        )
                    }
                })
            }

            is MemoBlockPayload.Latex -> JSONObject().apply {
                put("kind", "latex")
                put("version", PAYLOAD_VERSION)
                put("expression", payload.expression)
            }

            is MemoBlockPayload.Unknown -> JSONObject().apply {
                put("kind", "unknown")
                put("version", PAYLOAD_VERSION)
                put("rawType", payload.rawType)
                put("rawPayloadJson", payload.rawPayloadJson)
            }
        }
    }

    fun decode(type: MemoBlockType, payloadJson: JSONObject?): MemoBlockPayload {
        if (payloadJson == null) return MemoBlockPayload.None

        return runCatching {
            when (payloadJson.optString("kind")) {
                "heading" -> MemoBlockPayload.Heading(
                    level = parseHeadingLevel(payloadJson.optString("level")),
                    text = payloadJson.optString("text")
                )

                "toggle" -> MemoBlockPayload.Toggle(
                    title = payloadJson.optString("title"),
                    initiallyExpanded = payloadJson.optBoolean("initiallyExpanded", true),
                    childBlocks = decodeToggleChildren(payloadJson.optJSONArray("childBlocks"))
                )

                "quote" -> MemoBlockPayload.Quote(
                    text = payloadJson.optString("text")
                )

                "code" -> MemoBlockPayload.Code(
                    language = payloadJson.optString("language", "Plain Text").ifBlank { "Plain Text" },
                    code = payloadJson.optString("code")
                )

                "divider" -> MemoBlockPayload.Divider

                "linkCard" -> MemoBlockPayload.LinkCard(
                    url = payloadJson.optString("url"),
                    title = payloadJson.optString("title"),
                    description = payloadJson.optString("description"),
                    imageUrl = payloadJson.optString("imageUrl"),
                    faviconUrl = payloadJson.optString("faviconUrl")
                )

                "table" -> MemoBlockPayload.Table(
                    rows = decodeRows(payloadJson.optJSONArray("rows"))
                )

                "conversation" -> MemoBlockPayload.Conversation(
                    items = decodeConversationItems(payloadJson.optJSONArray("items"))
                )

                "latex" -> MemoBlockPayload.Latex(
                    expression = payloadJson.optString("expression")
                )

                "unknown" -> MemoBlockPayload.Unknown(
                    rawType = payloadJson.optString("rawType", type.name),
                    rawPayloadJson = payloadJson.optString("rawPayloadJson", payloadJson.toString())
                )

                else -> MemoBlockPayload.None
            }
        }.getOrElse {
            MemoBlockPayload.Unknown(
                rawType = type.name,
                rawPayloadJson = payloadJson.toString()
            )
        }
    }

    private fun decodeToggleChildren(array: JSONArray?): List<ToggleChildBlock> {
        if (array == null) return emptyList()
        return buildList {
            repeat(array.length()) { index ->
                val json = array.optJSONObject(index) ?: return@repeat
                add(
                    ToggleChildBlock(
                        id = json.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        type = parseMemoBlockType(json.optString("type"), MemoBlockType.Text),
                        text = json.optString("text")
                    )
                )
            }
        }
    }

    private fun decodeRows(array: JSONArray?): List<TableRow> {
        if (array == null || array.length() == 0) {
            return listOf(TableRow(cells = listOf("", "")), TableRow(cells = listOf("", "")))
        }
        return buildList {
            repeat(array.length()) { index ->
                val rowJson = array.optJSONObject(index) ?: return@repeat
                val cellsJson = rowJson.optJSONArray("cells") ?: JSONArray()
                val cells = buildList {
                    repeat(cellsJson.length()) { cellIndex ->
                        add(cellsJson.optString(cellIndex))
                    }
                }.ifEmpty { listOf("") }
                add(
                    TableRow(
                        id = rowJson.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        cells = cells
                    )
                )
            }
        }
    }

    private fun decodeConversationItems(array: JSONArray?): List<ConversationItem> {
        if (array == null || array.length() == 0) {
            return listOf(ConversationItem(speaker = "A", text = "", role = ConversationRole.Left))
        }
        return buildList {
            repeat(array.length()) { index ->
                val itemJson = array.optJSONObject(index) ?: return@repeat
                add(
                    ConversationItem(
                        id = itemJson.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        speaker = itemJson.optString("speaker"),
                        text = itemJson.optString("text"),
                        role = parseConversationRole(itemJson.optString("role"))
                    )
                )
            }
        }
    }

    private fun parseHeadingLevel(value: String): HeadingLevel {
        return runCatching { HeadingLevel.valueOf(value) }.getOrDefault(HeadingLevel.H1)
    }

    private fun parseConversationRole(value: String): ConversationRole {
        return runCatching { ConversationRole.valueOf(value) }.getOrDefault(ConversationRole.Left)
    }

    private fun parseMemoBlockType(value: String, fallback: MemoBlockType): MemoBlockType {
        return runCatching { MemoBlockType.valueOf(value) }.getOrDefault(fallback)
    }
}

