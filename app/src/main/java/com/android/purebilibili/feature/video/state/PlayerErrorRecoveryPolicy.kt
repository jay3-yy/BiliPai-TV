package com.android.purebilibili.feature.video.state

import androidx.media3.common.PlaybackException

internal enum class PlayerErrorRecoveryAction {
    SWITCH_CDN,
    RETRY_NETWORK,
    RETRY_DECODER_FALLBACK,
    RETRY_NON_NETWORK,
    GIVE_UP
}

internal fun decidePlayerErrorRecovery(
    errorCode: Int,
    hasCdnAlternatives: Boolean,
    retryCount: Int,
    maxRetries: Int,
    cdnSwitchCount: Int,
    maxCdnSwitches: Int,
    isDecoderLikeFailure: Boolean
): PlayerErrorRecoveryAction {
    return if (isNetworkPlaybackError(errorCode)) {
        when {
            hasCdnAlternatives && cdnSwitchCount < maxCdnSwitches -> PlayerErrorRecoveryAction.SWITCH_CDN
            retryCount < maxRetries -> PlayerErrorRecoveryAction.RETRY_NETWORK
            else -> PlayerErrorRecoveryAction.GIVE_UP
        }
    } else {
        when {
            isDecoderLikeFailure && retryCount < 1 -> PlayerErrorRecoveryAction.RETRY_DECODER_FALLBACK
            retryCount < 1 -> PlayerErrorRecoveryAction.RETRY_NON_NETWORK
            else -> PlayerErrorRecoveryAction.GIVE_UP
        }
    }
}

internal fun isNetworkPlaybackError(errorCode: Int): Boolean {
    return errorCode in listOf(
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
    )
}

internal fun isDecoderLikeFailure(
    errorMessage: String?,
    causeClassName: String?
): Boolean {
    val message = errorMessage.orEmpty().lowercase()
    val cause = causeClassName.orEmpty().lowercase()
    return message.contains("decoder") ||
        message.contains("mediacodec") ||
        message.contains("renderer") ||
        cause.contains("decoder") ||
        cause.contains("mediacodec") ||
        cause.contains("renderer")
}
