package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertTrue
import org.junit.Test

class MineSideDrawerVisualPolicyTest {

    @Test
    fun `blur-enabled drawer should keep translucent glass surface`() {
        val light = resolveDrawerGlassPalette(isDark = false, blurEnabled = true)
        val dark = resolveDrawerGlassPalette(isDark = true, blurEnabled = true)

        assertTrue(light.drawerBaseAlpha <= 0.34f)
        assertTrue(dark.drawerBaseAlpha <= 0.38f)
        assertTrue(light.itemSurfaceAlpha <= 0.22f)
        assertTrue(dark.itemSurfaceAlpha <= 0.20f)
    }

    @Test
    fun `blur-disabled drawer can stay opaque for readability`() {
        val light = resolveDrawerGlassPalette(isDark = false, blurEnabled = false)
        val dark = resolveDrawerGlassPalette(isDark = true, blurEnabled = false)

        assertTrue(light.drawerBaseAlpha >= 0.92f)
        assertTrue(dark.drawerBaseAlpha >= 0.92f)
    }

    @Test
    fun `drawer scrim should stay light when blur is enabled`() {
        val blurScrim = resolveHomeDrawerScrimAlpha(blurEnabled = true)
        val opaqueScrim = resolveHomeDrawerScrimAlpha(blurEnabled = false)

        assertTrue(blurScrim <= 0.16f)
        assertTrue(opaqueScrim >= 0.24f)
    }
}
