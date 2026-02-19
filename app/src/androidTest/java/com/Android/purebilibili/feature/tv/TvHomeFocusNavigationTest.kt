package com.android.purebilibili.feature.tv

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.home.HomeTvFocusZone
import com.android.purebilibili.feature.home.resolveInitialHomeTvFocusZone
import com.android.purebilibili.feature.home.resolveHomeTvFocusTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvHomeFocusNavigationTest {

    @Test
    fun tvInitialFocus_defaultsToSidebarWhenAvailable() {
        val initial = resolveInitialHomeTvFocusZone(isTv = true, hasSidebar = true)
        assertEquals(HomeTvFocusZone.SIDEBAR, initial)
    }

    @Test
    fun pagerDown_movesToGrid() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.PAGER,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = false,
            isGridFirstColumn = false
        )

        assertEquals(HomeTvFocusZone.GRID, transition.nextZone)
        assertTrue(transition.consumeEvent)
    }

    @Test
    fun pagerLeft_movesToSidebar_whenSidebarEnabled() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.PAGER,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = false,
            isGridFirstColumn = false
        )

        assertEquals(HomeTvFocusZone.SIDEBAR, transition.nextZone)
        assertTrue(transition.consumeEvent)
    }

    @Test
    fun gridFirstRowUp_returnsPager() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.GRID,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = true,
            isGridFirstColumn = false
        )

        assertEquals(HomeTvFocusZone.PAGER, transition.nextZone)
        assertTrue(transition.consumeEvent)
    }

    @Test
    fun sidebarRight_returnsPager() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.SIDEBAR,
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = false,
            isGridFirstColumn = false
        )

        assertEquals(HomeTvFocusZone.PAGER, transition.nextZone)
        assertTrue(transition.consumeEvent)
    }
}
