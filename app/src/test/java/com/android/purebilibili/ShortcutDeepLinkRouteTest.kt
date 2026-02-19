package com.android.purebilibili

import com.android.purebilibili.navigation.ScreenRoutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShortcutDeepLinkRouteTest {

    @Test
    fun resolveShortcutRoute_supportsReleaseSmokeEntries() {
        assertEquals(ScreenRoutes.Login.route, resolveShortcutRoute("login"))
        assertEquals(ScreenRoutes.PlaybackSettings.route, resolveShortcutRoute("playback"))
        assertEquals(ScreenRoutes.PluginsSettings.route, resolveShortcutRoute("plugins"))
    }

    @Test
    fun resolveShortcutRoute_returnsNullForUnknownHost() {
        assertNull(resolveShortcutRoute("unknown"))
    }
}
