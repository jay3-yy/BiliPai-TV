package com.android.purebilibili.feature.video.usecase

import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackUseCaseQualitySwitchTest {

    private val cachedVideos = listOf(
        DashVideo(id = 80, baseUrl = "https://example.com/1080.m4s"),
        DashVideo(id = 64, baseUrl = "https://example.com/720.m4s"),
        DashVideo(id = 32, baseUrl = "https://example.com/480.m4s")
    )

    private val cachedAudios = listOf(
        DashAudio(id = 30280, baseUrl = "https://example.com/audio.m4s")
    )

    @Test
    fun `changeQualityFromCache returns null when target quality not cached`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.changeQualityFromCache(
            qualityId = 120,
            cachedVideos = cachedVideos,
            cachedAudios = cachedAudios,
            currentPos = 0L
        )

        assertNull(result)
    }

    @Test
    fun `changeQualityFromCache returns exact match when target quality exists`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.changeQualityFromCache(
            qualityId = 64,
            cachedVideos = cachedVideos,
            cachedAudios = cachedAudios,
            currentPos = 0L
        )

        assertNotNull(result)
        assertEquals(64, result?.actualQuality)
    }

    @Test
    fun `mergeQualityOptions keeps api high tiers when dash list misses them`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.mergeQualityOptions(
            apiQualities = listOf(120, 116, 80, 64, 32, 16),
            dashVideoIds = listOf(80, 64, 32, 16)
        )

        assertEquals(listOf(120, 116, 80, 64, 32, 16), result.mergedQualityIds)
        assertEquals(listOf(120, 116), result.apiOnlyHighQualities)
    }

    @Test
    fun `mergeQualityOptions uses dash list when api list is empty`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.mergeQualityOptions(
            apiQualities = emptyList(),
            dashVideoIds = listOf(80, 64)
        )

        assertEquals(listOf(80, 64, 32, 16), result.mergedQualityIds)
        assertTrue(result.apiOnlyHighQualities.isEmpty())
    }
}
