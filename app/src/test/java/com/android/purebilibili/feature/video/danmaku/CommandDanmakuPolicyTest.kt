package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CommandDanmakuPolicyTest {

    @Test
    fun `build command danmaku with plain text content`() {
        val cmd = commandDm(
            command = "VIDEO_CONNECTION_MSG",
            content = "高能预警！"
        )

        val result = buildCommandDanmaku(cmd)

        assertNotNull(result)
        assertEquals("高能预警！", result.content)
        assertEquals(5000, result.durationMs)
    }

    @Test
    fun `extract display text from json content`() {
        val cmd = commandDm(
            content = """{"text":"这条是可读互动提示"}"""
        )

        val result = buildCommandDanmaku(cmd)

        assertNotNull(result)
        assertEquals("这条是可读互动提示", result.content)
    }

    @Test
    fun `filter structured payload gibberish`() {
        val cmd = commandDm(
            content = """"453dc8b380c6dba.png","type":2,"upower_state":1"""
        )

        val result = buildCommandDanmaku(cmd)

        assertNull(result)
    }

    @Test
    fun `filter non visual command type`() {
        val cmd = commandDm(
            command = "UPOWER_STATE",
            content = "这条文本不应展示"
        )

        val result = buildCommandDanmaku(cmd)

        assertNull(result)
    }

    private fun commandDm(
        command: String = "",
        content: String = "",
        extra: String = "",
        progress: Int = 1000
    ): DanmakuProto.CommandDm {
        return DanmakuProto.CommandDm(
            id = 1L,
            command = command,
            content = content,
            extra = extra,
            progress = progress
        )
    }
}

