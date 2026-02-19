package com.android.purebilibili.core.util

import android.media.MediaCodecList

object MediaUtils {
    /**
     * Check if HEVC (H.265) decoder is supported
     */
    fun isHevcSupported(): Boolean {
        return hasDecoder("video/hevc")
    }

    /**
     * Check if AV1 decoder is supported
     */
    fun isAv1Supported(): Boolean {
        // AV1 support is limited on older devices
        return hasDecoder("video/av01")
    }

    /**
     * Check if HDR (HDR10/HLG) video is supported
     * HDR requires both decoder support and display capability
     */
    fun isHdrSupported(): Boolean {
        // HDR10 uses HEVC with specific profile
        // Check for HEVC support first, then assume HDR display is available on modern devices
        return hasDecoder("video/hevc") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
    }
    
    /**
     * Check if Dolby Vision is supported
     * Dolby Vision requires specific hardware decoder
     */
    fun isDolbyVisionSupported(): Boolean {
        // Dolby Vision MIME type
        val hasDolbyDecoder = hasDecoder("video/dolby-vision") || hasDecoder("video/dvhe") || hasDecoder("video/dvav")
        return hasDolbyDecoder && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
    }

    private fun hasDecoder(mimeType: String): Boolean {
        try {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val codecs = list.codecInfos
            for (codec in codecs) {
                if (codec.isEncoder) continue
                val types = codec.supportedTypes
                for (type in types) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("MediaUtils", "Failed to check decoder support for $mimeType", e)
        }
        return false
    }
}
