package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.SessionAccountInfo
import com.android.purebilibili.data.model.response.SessionItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InboxUserInfoResolverTest {

    @Test
    fun resolveDisplayName_fallsBackToSessionWhenCachedNameIsBlank() {
        val session = SessionItem(
            talker_id = 1001L,
            account_info = SessionAccountInfo(name = "会话用户名")
        )
        val cached = UserBasicInfo(mid = 1001L, name = "   ", face = "https://cdn.example/avatar.jpg")

        val displayName = InboxUserInfoResolver.resolveDisplayName(cached, session)

        assertEquals("会话用户名", displayName)
    }

    @Test
    fun resolveDisplayAvatar_fallsBackToSessionAndNormalizesProtocolWhenCachedFaceIsBlank() {
        val session = SessionItem(
            talker_id = 1002L,
            account_info = SessionAccountInfo(pic_url = "//i0.hdslb.com/bfs/face/abc.png")
        )
        val cached = UserBasicInfo(mid = 1002L, name = "用户", face = "")

        val displayAvatar = InboxUserInfoResolver.resolveDisplayAvatar(cached, session)

        assertTrue(displayAvatar.startsWith("https://"))
    }

    @Test
    fun shouldFetchUserInfo_returnsTrueWhenCacheMissingOrIncomplete() {
        val mid = 1003L
        val missingCache = emptyMap<Long, UserBasicInfo>()
        val incompleteCache = mapOf(
            mid to UserBasicInfo(mid = mid, name = "用户", face = "")
        )

        assertTrue(InboxUserInfoResolver.shouldFetchUserInfo(mid, missingCache))
        assertTrue(InboxUserInfoResolver.shouldFetchUserInfo(mid, incompleteCache))
    }

    @Test
    fun shouldFetchUserInfo_returnsFalseWhenCacheComplete() {
        val mid = 1004L
        val completeCache = mapOf(
            mid to UserBasicInfo(mid = mid, name = "完整用户", face = "https://cdn.example/avatar.jpg")
        )

        assertFalse(InboxUserInfoResolver.shouldFetchUserInfo(mid, completeCache))
    }

    @Test
    fun mergeFetchedUserInfo_keepsExistingFieldsWhenIncomingIsBlank() {
        val existing = UserBasicInfo(
            mid = 1005L,
            name = "旧用户名",
            face = "https://cdn.example/old.jpg"
        )
        val fetched = UserBasicInfo(
            mid = 1005L,
            name = "",
            face = "   "
        )

        val merged = InboxUserInfoResolver.mergeFetchedUserInfo(existing, fetched)

        assertEquals("旧用户名", merged?.name)
        assertEquals("https://cdn.example/old.jpg", merged?.face)
    }
}
