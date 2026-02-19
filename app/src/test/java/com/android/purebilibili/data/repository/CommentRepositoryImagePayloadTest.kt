package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.ReplyPicture
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentRepositoryImagePayloadTest {

    @Test
    fun `buildPicturesPayload returns null when no pictures`() {
        val payload = CommentRepository.buildPicturesPayload(emptyList())
        assertNull(payload)
    }

    @Test
    fun `buildPicturesPayload serializes expected fields`() {
        val payload = CommentRepository.buildPicturesPayload(
            listOf(
                ReplyPicture(
                    imgSrc = "https://i0.hdslb.com/bfs/new_dyn/test.png",
                    imgWidth = 1080,
                    imgHeight = 720,
                    imgSize = 321.5f
                )
            )
        )

        requireNotNull(payload)
        assertTrue(payload.contains("\"img_src\":\"https://i0.hdslb.com/bfs/new_dyn/test.png\""))
        assertTrue(payload.contains("\"img_width\":1080"))
        assertTrue(payload.contains("\"img_height\":720"))
        assertTrue(payload.contains("\"img_size\":321.5"))
    }
}
