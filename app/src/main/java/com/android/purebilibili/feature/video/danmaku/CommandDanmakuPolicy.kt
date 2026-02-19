package com.android.purebilibili.feature.video.danmaku

import org.json.JSONObject

private val NON_VISUAL_COMMAND_TYPES = setOf(
    "UPOWER_STATE",
    "UPGRADE_STATE",
    "PANEL_STATE"
)

private val TEXT_FIELD_CANDIDATES = listOf(
    "text",
    "content",
    "msg",
    "message",
    "title"
)

internal fun buildCommandDanmaku(cmd: DanmakuProto.CommandDm): AdvancedDanmakuData? {
    val text = resolveCommandDanmakuText(cmd) ?: return null
    return AdvancedDanmakuData(
        id = "cmd_${cmd.id}",
        content = text,
        startTimeMs = cmd.progress.coerceAtLeast(0).toLong(),
        durationMs = 5000,
        startX = 0.5f,
        startY = 0.1f,
        fontSize = 20f,
        color = 0xFFD700,
        alpha = 0.9f
    )
}

internal fun resolveCommandDanmakuText(cmd: DanmakuProto.CommandDm): String? {
    val commandType = cmd.command.trim().uppercase()
    if (commandType in NON_VISUAL_COMMAND_TYPES) return null
    return extractReadableCommandText(cmd.content)
        ?: extractReadableCommandText(cmd.extra)
}

private fun extractReadableCommandText(raw: String): String? {
    val content = raw.trim()
    if (content.isEmpty()) return null

    if (looksLikeJson(content)) {
        return extractTextFromJson(content)
    }

    return sanitizeText(content)
}

private fun looksLikeJson(content: String): Boolean {
    val trimmed = content.trim()
    return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
        (trimmed.startsWith("[") && trimmed.endsWith("]"))
}

private fun extractTextFromJson(rawJson: String): String? {
    TEXT_FIELD_CANDIDATES.firstNotNullOfOrNull { key ->
        Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
            .find(rawJson)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::sanitizeText)
    }?.let { return it }

    return try {
        val json = JSONObject(rawJson)

        TEXT_FIELD_CANDIDATES.firstNotNullOfOrNull { key ->
            sanitizeText(json.optString(key).orEmpty())
        } ?: run {
            val nested = json.optJSONObject("data")
                ?: json.optJSONObject("extra")
            nested?.let { nestedJson ->
                TEXT_FIELD_CANDIDATES.firstNotNullOfOrNull { key ->
                    sanitizeText(nestedJson.optString(key).orEmpty())
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun sanitizeText(raw: String): String? {
    val normalized = raw.replace('\n', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    if (normalized.isEmpty()) return null
    if (normalized.contains("upower_state", ignoreCase = true)) return null
    if (normalized.contains("\"type\":", ignoreCase = true)) return null
    if (normalized.contains("\",\"type\":", ignoreCase = true)) return null
    if (normalized.contains(".png\"", ignoreCase = true)) return null

    val punctuationDensity = normalized.count { it == ':' || it == ',' || it == '"' }
    if (normalized.length > 32 && punctuationDensity >= 4) return null

    return normalized
}
