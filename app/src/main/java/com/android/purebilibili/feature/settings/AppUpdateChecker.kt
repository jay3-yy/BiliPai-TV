package com.android.purebilibili.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateCheckResult(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val publishedAt: String?,
    val message: String
)

object AppUpdateChecker {
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/jay3-yy/BiliPai/releases/latest"
    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 8000

    suspend fun check(currentVersion: String): Result<AppUpdateCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "BiliPai-UpdateChecker")
            }
            try {
                val conn = connection
                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("更新接口异常: HTTP $responseCode")
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                val latestTag = json.optString("tag_name", "")
                val latestVersion = normalizeVersion(latestTag)
                if (latestVersion.isEmpty()) {
                    throw IllegalStateException("未获取到有效版本号")
                }

                val releaseUrl = json.optString("html_url", "https://github.com/jay3-yy/BiliPai/releases")
                val releaseNotes = json.optString("body", "").trim()
                val publishedAt = json.optString("published_at", "").takeIf { it.isNotBlank() }
                val updateAvailable = isRemoteNewer(currentVersion, latestVersion)
                val message = if (updateAvailable) {
                    "发现新版本 v$latestVersion"
                } else {
                    "已是最新版本"
                }

                AppUpdateCheckResult(
                    isUpdateAvailable = updateAvailable,
                    currentVersion = normalizeVersion(currentVersion),
                    latestVersion = latestVersion,
                    releaseUrl = releaseUrl,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt,
                    message = message
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun normalizeVersion(version: String): String {
        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .trim()
    }

    internal fun isRemoteNewer(localVersion: String, remoteVersion: String): Boolean {
        val local = parseVersionParts(normalizeVersion(localVersion))
        val remote = parseVersionParts(normalizeVersion(remoteVersion))
        val maxSize = maxOf(local.size, remote.size)
        for (index in 0 until maxSize) {
            val localPart = local.getOrElse(index) { 0 }
            val remotePart = remote.getOrElse(index) { 0 }
            if (remotePart > localPart) return true
            if (remotePart < localPart) return false
        }
        return false
    }

    internal fun parseVersionParts(version: String): List<Int> {
        if (version.isBlank()) return emptyList()
        return version
            .split('.')
            .mapNotNull { part -> part.toIntOrNull() }
    }
}
