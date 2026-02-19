package com.android.purebilibili.feature.home

import android.view.KeyEvent

internal enum class HomeTvFocusZone {
    SIDEBAR,
    PAGER,
    GRID
}

internal data class HomeTvFocusTransition(
    val nextZone: HomeTvFocusZone,
    val consumeEvent: Boolean,
    val pageDelta: Int = 0
)

internal data class HomeTvRootNavigationCommand(
    val nextZone: HomeTvFocusZone,
    val targetPage: Int?
)

internal fun resolveInitialHomeTvFocusZone(
    isTv: Boolean,
    hasSidebar: Boolean
): HomeTvFocusZone? {
    if (!isTv) return null
    return if (hasSidebar) HomeTvFocusZone.SIDEBAR else HomeTvFocusZone.PAGER
}

internal fun resolveHomeTvFocusTransition(
    currentZone: HomeTvFocusZone,
    keyCode: Int,
    action: Int,
    hasSidebar: Boolean,
    isGridFirstRow: Boolean,
    isGridFirstColumn: Boolean
): HomeTvFocusTransition {
    if (action != KeyEvent.ACTION_DOWN) {
        return HomeTvFocusTransition(nextZone = currentZone, consumeEvent = false)
    }

    return when (currentZone) {
        HomeTvFocusZone.SIDEBAR -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                HomeTvFocusTransition(nextZone = HomeTvFocusZone.PAGER, consumeEvent = true)
            } else {
                HomeTvFocusTransition(nextZone = currentZone, consumeEvent = false)
            }
        }

        HomeTvFocusZone.PAGER -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                HomeTvFocusTransition(nextZone = HomeTvFocusZone.GRID, consumeEvent = true)
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (hasSidebar) {
                    HomeTvFocusTransition(nextZone = HomeTvFocusZone.SIDEBAR, consumeEvent = true)
                } else {
                    HomeTvFocusTransition(nextZone = HomeTvFocusZone.PAGER, consumeEvent = true, pageDelta = -1)
                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                HomeTvFocusTransition(nextZone = HomeTvFocusZone.PAGER, consumeEvent = true, pageDelta = 1)
            }

            else -> HomeTvFocusTransition(nextZone = currentZone, consumeEvent = false)
        }

        HomeTvFocusZone.GRID -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isGridFirstRow) {
                    HomeTvFocusTransition(nextZone = HomeTvFocusZone.PAGER, consumeEvent = true)
                } else {
                    HomeTvFocusTransition(nextZone = currentZone, consumeEvent = false)
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (hasSidebar && isGridFirstColumn) {
                    HomeTvFocusTransition(nextZone = HomeTvFocusZone.SIDEBAR, consumeEvent = true)
                } else {
                    HomeTvFocusTransition(nextZone = currentZone, consumeEvent = false)
                }
            }

            else -> HomeTvFocusTransition(nextZone = currentZone, consumeEvent = false)
        }
    }
}

internal fun resolveHomeTvRootNavigationCommand(
    currentZone: HomeTvFocusZone,
    keyCode: Int,
    action: Int,
    hasSidebar: Boolean,
    currentPage: Int,
    pageCount: Int
): HomeTvRootNavigationCommand? {
    if (currentZone == HomeTvFocusZone.GRID) return null
    val transition = resolveHomeTvFocusTransition(
        currentZone = currentZone,
        keyCode = keyCode,
        action = action,
        hasSidebar = hasSidebar,
        isGridFirstRow = false,
        isGridFirstColumn = false
    )
    if (!transition.consumeEvent) return null

    val targetPage = if (transition.pageDelta != 0) {
        val candidate = currentPage + transition.pageDelta
        if (candidate in 0 until pageCount) candidate else return null
    } else {
        null
    }

    return HomeTvRootNavigationCommand(
        nextZone = transition.nextZone,
        targetPage = targetPage
    )
}

internal fun resolveHomeTvGridCellTransition(
    index: Int,
    gridColumns: Int,
    keyCode: Int,
    action: Int,
    hasSidebar: Boolean
): HomeTvFocusTransition? {
    if (gridColumns <= 0 || index < 0) return null
    val transition = resolveHomeTvFocusTransition(
        currentZone = HomeTvFocusZone.GRID,
        keyCode = keyCode,
        action = action,
        hasSidebar = hasSidebar,
        isGridFirstRow = index < gridColumns,
        isGridFirstColumn = index % gridColumns == 0
    )
    return transition.takeIf { it.consumeEvent }
}
