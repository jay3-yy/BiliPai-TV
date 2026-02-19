package com.android.purebilibili.feature.search

data class SearchLayoutPolicy(
    val resultGridMinItemWidthDp: Int,
    val resultGridSpacingDp: Int,
    val resultHorizontalPaddingDp: Int,
    val splitOuterPaddingDp: Int,
    val splitInnerGapDp: Int,
    val leftPaneWeight: Float,
    val rightPaneWeight: Float,
    val hotSearchColumns: Int
)

fun shouldUseSearchSplitLayout(
    widthDp: Int,
    isTv: Boolean
): Boolean = isTv || widthDp >= 840

fun resolveSearchLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): SearchLayoutPolicy {
    if (isTv) {
        return SearchLayoutPolicy(
            resultGridMinItemWidthDp = 220,
            resultGridSpacingDp = 12,
            resultHorizontalPaddingDp = 20,
            splitOuterPaddingDp = 24,
            splitInnerGapDp = 12,
            leftPaneWeight = 1f,
            rightPaneWeight = 1f,
            hotSearchColumns = 2
        )
    }

    return when {
        widthDp >= 1600 -> SearchLayoutPolicy(
            resultGridMinItemWidthDp = 260,
            resultGridSpacingDp = 16,
            resultHorizontalPaddingDp = 24,
            splitOuterPaddingDp = 32,
            splitInnerGapDp = 16,
            leftPaneWeight = 1.15f,
            rightPaneWeight = 0.85f,
            hotSearchColumns = 4
        )
        widthDp >= 840 -> SearchLayoutPolicy(
            resultGridMinItemWidthDp = 220,
            resultGridSpacingDp = 12,
            resultHorizontalPaddingDp = 20,
            splitOuterPaddingDp = 24,
            splitInnerGapDp = 12,
            leftPaneWeight = 1.05f,
            rightPaneWeight = 0.95f,
            hotSearchColumns = 3
        )
        widthDp >= 600 -> SearchLayoutPolicy(
            resultGridMinItemWidthDp = 200,
            resultGridSpacingDp = 12,
            resultHorizontalPaddingDp = 16,
            splitOuterPaddingDp = 20,
            splitInnerGapDp = 12,
            leftPaneWeight = 1f,
            rightPaneWeight = 1f,
            hotSearchColumns = 2
        )
        else -> SearchLayoutPolicy(
            resultGridMinItemWidthDp = 160,
            resultGridSpacingDp = 8,
            resultHorizontalPaddingDp = 8,
            splitOuterPaddingDp = 16,
            splitInnerGapDp = 8,
            leftPaneWeight = 1f,
            rightPaneWeight = 1f,
            hotSearchColumns = 2
        )
    }
}
