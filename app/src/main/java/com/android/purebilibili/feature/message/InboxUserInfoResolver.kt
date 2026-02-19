package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.SessionItem

internal object InboxUserInfoResolver {

    fun resolveDisplayName(cached: UserBasicInfo?, session: SessionItem): String {
        val cachedName = cached?.name.cleanValue()
        if (cachedName.isNotEmpty()) return cachedName

        val sessionName = session.account_info?.name.cleanValue()
        if (sessionName.isNotEmpty()) return sessionName

        return "用户${session.talker_id}"
    }

    fun resolveDisplayAvatar(cached: UserBasicInfo?, session: SessionItem): String {
        val cachedAvatar = normalizeAvatarUrl(cached?.face)
        if (cachedAvatar.isNotEmpty()) return cachedAvatar

        return normalizeAvatarUrl(session.account_info?.avatarUrl)
    }

    fun shouldFetchUserInfo(mid: Long, cache: Map<Long, UserBasicInfo>): Boolean {
        val cached = cache[mid] ?: return true
        return cached.name.cleanValue().isEmpty() || normalizeAvatarUrl(cached.face).isEmpty()
    }

    fun mergeFetchedUserInfo(existing: UserBasicInfo?, fetched: UserBasicInfo?): UserBasicInfo? {
        if (fetched == null) return existing

        val mergedMid = if (fetched.mid > 0) fetched.mid else existing?.mid ?: 0L
        if (mergedMid <= 0L) return existing

        val mergedName = fetched.name.cleanValue().ifEmpty { existing?.name.cleanValue() }
        val mergedFace = normalizeAvatarUrl(fetched.face).ifEmpty { normalizeAvatarUrl(existing?.face) }

        if (mergedName.isEmpty() && mergedFace.isEmpty()) {
            return existing
        }

        return UserBasicInfo(
            mid = mergedMid,
            name = mergedName,
            face = mergedFace
        )
    }

    private fun String?.cleanValue(): String = this?.trim().orEmpty()

    private fun normalizeAvatarUrl(raw: String?): String {
        val value = raw.cleanValue()
        if (value.isEmpty()) return ""
        return when {
            value.startsWith("//") -> "https:$value"
            value.startsWith("http://") -> value.replaceFirst("http://", "https://")
            else -> value
        }
    }
}
