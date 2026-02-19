package com.android.purebilibili.feature.video.state

import androidx.media3.common.PlaybackException
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerErrorRecoveryPolicyTest {

    @Test
    fun networkErrorPrefersCdnSwitchWhenAlternativesExist() {
        val action = decidePlayerErrorRecovery(
            errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            hasCdnAlternatives = true,
            retryCount = 0,
            maxRetries = 3,
            cdnSwitchCount = 0,
            maxCdnSwitches = 2,
            isDecoderLikeFailure = false
        )

        assertEquals(PlayerErrorRecoveryAction.SWITCH_CDN, action)
    }

    @Test
    fun networkErrorRetriesWhenNoCdnAlternatives() {
        val action = decidePlayerErrorRecovery(
            errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            hasCdnAlternatives = false,
            retryCount = 0,
            maxRetries = 3,
            cdnSwitchCount = 2,
            maxCdnSwitches = 2,
            isDecoderLikeFailure = false
        )

        assertEquals(PlayerErrorRecoveryAction.RETRY_NETWORK, action)
    }

    @Test
    fun decoderLikeFailureFallsBackToSafeCodecFirst() {
        val action = decidePlayerErrorRecovery(
            errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
            hasCdnAlternatives = false,
            retryCount = 0,
            maxRetries = 3,
            cdnSwitchCount = 0,
            maxCdnSwitches = 2,
            isDecoderLikeFailure = true
        )

        assertEquals(PlayerErrorRecoveryAction.RETRY_DECODER_FALLBACK, action)
    }

    @Test
    fun nonNetworkNonDecoderRetriesOnce() {
        val action = decidePlayerErrorRecovery(
            errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
            hasCdnAlternatives = false,
            retryCount = 0,
            maxRetries = 3,
            cdnSwitchCount = 0,
            maxCdnSwitches = 2,
            isDecoderLikeFailure = false
        )

        assertEquals(PlayerErrorRecoveryAction.RETRY_NON_NETWORK, action)
    }

    @Test
    fun givesUpWhenRetryBudgetExhausted() {
        val action = decidePlayerErrorRecovery(
            errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            hasCdnAlternatives = false,
            retryCount = 3,
            maxRetries = 3,
            cdnSwitchCount = 2,
            maxCdnSwitches = 2,
            isDecoderLikeFailure = false
        )

        assertEquals(PlayerErrorRecoveryAction.GIVE_UP, action)
    }

    @Test
    fun detectsDecoderLikeFailureFromMessageAndCauseHints() {
        val fromMessage = isDecoderLikeFailure(
            errorMessage = "Decoder init failed: OMX.qcom.video.decoder.hevc",
            causeClassName = null
        )
        val fromCause = isDecoderLikeFailure(
            errorMessage = null,
            causeClassName = "androidx.media3.exoplayer.mediacodec.MediaCodecRenderer\$DecoderInitializationException"
        )

        assertEquals(true, fromMessage)
        assertEquals(true, fromCause)
    }
}
