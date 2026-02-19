package com.android.purebilibili.feature.search

import android.view.KeyEvent

internal enum class SearchTvFocusZone {
    TOP_BAR,
    SUGGESTIONS,
    RESULTS,
    HISTORY
}

internal data class SearchTvFocusTransition(
    val nextZone: SearchTvFocusZone,
    val consumeEvent: Boolean
)

private fun shouldHandleSearchTvFocusKey(
    keyCode: Int,
    action: Int
): Boolean {
    return when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT -> action == KeyEvent.ACTION_DOWN

        KeyEvent.KEYCODE_BACK -> action == KeyEvent.ACTION_UP
        else -> false
    }
}

internal fun resolveInitialSearchTvFocusZone(isTv: Boolean): SearchTvFocusZone? {
    return if (isTv) SearchTvFocusZone.TOP_BAR else null
}

internal fun resolveSearchTvFocusTransition(
    currentZone: SearchTvFocusZone,
    keyCode: Int,
    action: Int,
    showResults: Boolean,
    hasSuggestions: Boolean,
    hasHistory: Boolean
): SearchTvFocusTransition {
    if (!shouldHandleSearchTvFocusKey(keyCode = keyCode, action = action)) {
        return SearchTvFocusTransition(nextZone = currentZone, consumeEvent = false)
    }

    return when (currentZone) {
        SearchTvFocusZone.TOP_BAR -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val next = when {
                    !showResults && hasSuggestions -> SearchTvFocusZone.SUGGESTIONS
                    showResults -> SearchTvFocusZone.RESULTS
                    hasHistory -> SearchTvFocusZone.HISTORY
                    else -> SearchTvFocusZone.TOP_BAR
                }
                SearchTvFocusTransition(nextZone = next, consumeEvent = next != SearchTvFocusZone.TOP_BAR)
            } else {
                SearchTvFocusTransition(nextZone = currentZone, consumeEvent = false)
            }
        }

        SearchTvFocusZone.SUGGESTIONS -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_BACK) {
                SearchTvFocusTransition(nextZone = SearchTvFocusZone.TOP_BAR, consumeEvent = true)
            } else {
                SearchTvFocusTransition(nextZone = currentZone, consumeEvent = false)
            }
        }

        SearchTvFocusZone.RESULTS -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_BACK) {
                SearchTvFocusTransition(nextZone = SearchTvFocusZone.TOP_BAR, consumeEvent = true)
            } else {
                SearchTvFocusTransition(nextZone = currentZone, consumeEvent = false)
            }
        }

        SearchTvFocusZone.HISTORY -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_BACK) {
                SearchTvFocusTransition(nextZone = SearchTvFocusZone.TOP_BAR, consumeEvent = true)
            } else {
                SearchTvFocusTransition(nextZone = currentZone, consumeEvent = false)
            }
        }
    }
}

internal fun resolveSearchTvResultEntryIndex(
    isTv: Boolean,
    showResults: Boolean,
    resultCount: Int
): Int? {
    if (!isTv || !showResults || resultCount <= 0) return null
    return 0
}
