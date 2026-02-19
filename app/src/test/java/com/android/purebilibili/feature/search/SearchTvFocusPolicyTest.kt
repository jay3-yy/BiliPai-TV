package com.android.purebilibili.feature.search

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTvFocusPolicyTest {

    @Test
    fun initialFocus_defaultsToTopBarOnTv() {
        assertEquals(SearchTvFocusZone.TOP_BAR, resolveInitialSearchTvFocusZone(isTv = true))
    }

    @Test
    fun downFromTopMovesToSuggestionsWhenVisible() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.TOP_BAR,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_DOWN,
            showResults = false,
            hasSuggestions = true,
            hasHistory = true
        )

        assertEquals(SearchTvFocusZone.SUGGESTIONS, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun downFromTopMovesToResultsWhenResultsVisible() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.TOP_BAR,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_DOWN,
            showResults = true,
            hasSuggestions = false,
            hasHistory = false
        )

        assertEquals(SearchTvFocusZone.RESULTS, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun upFromSuggestionsReturnsTop() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.SUGGESTIONS,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_DOWN,
            showResults = false,
            hasSuggestions = true,
            hasHistory = true
        )

        assertEquals(SearchTvFocusZone.TOP_BAR, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun upFromResultsReturnsTop() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.RESULTS,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_DOWN,
            showResults = true,
            hasSuggestions = false,
            hasHistory = false
        )

        assertEquals(SearchTvFocusZone.TOP_BAR, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun backFromResultsReturnsTopInsteadOfLeavingPage() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.RESULTS,
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_UP,
            showResults = true,
            hasSuggestions = false,
            hasHistory = false
        )

        assertEquals(SearchTvFocusZone.TOP_BAR, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun backFromResultsOnKeyDown_doesNotTriggerBackTransition() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.RESULTS,
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_DOWN,
            showResults = true,
            hasSuggestions = false,
            hasHistory = false
        )

        assertEquals(SearchTvFocusZone.RESULTS, transition.nextZone)
        assertEquals(false, transition.consumeEvent)
    }

    @Test
    fun resultEntryFocus_defaultsToFirstResultOnTv() {
        assertEquals(
            0,
            resolveSearchTvResultEntryIndex(
                isTv = true,
                showResults = true,
                resultCount = 8
            )
        )
    }

    @Test
    fun resultEntryFocus_ignoresNonTvOrEmptyResultState() {
        assertEquals(
            null,
            resolveSearchTvResultEntryIndex(
                isTv = false,
                showResults = true,
                resultCount = 8
            )
        )
        assertEquals(
            null,
            resolveSearchTvResultEntryIndex(
                isTv = true,
                showResults = false,
                resultCount = 8
            )
        )
        assertEquals(
            null,
            resolveSearchTvResultEntryIndex(
                isTv = true,
                showResults = true,
                resultCount = 0
            )
        )
    }
}
