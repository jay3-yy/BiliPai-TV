package com.android.purebilibili.feature.plugin

internal data class EyeCareTuning(
    val brightnessLevel: Float,
    val warmFilterStrength: Float,
    val reminderIntervalMinutes: Int
)

internal data class EyeVisualState(
    val isActive: Boolean,
    val brightnessLevel: Float,
    val warmFilterStrength: Float
)

internal fun isWithinProtectionWindow(
    currentHour: Int,
    startHour: Int,
    endHour: Int
): Boolean {
    return if (startHour > endHour) {
        currentHour >= startHour || currentHour < endHour
    } else if (startHour < endHour) {
        currentHour >= startHour && currentHour < endHour
    } else {
        // start == end means full-day mode
        true
    }
}

internal fun shouldTriggerCareReminder(
    usageMinutes: Int,
    intervalMinutes: Int,
    snoozeUntilMinute: Int?,
    lastReminderMinute: Int?
): Boolean {
    if (intervalMinutes <= 0 || usageMinutes <= 0) return false
    if (usageMinutes % intervalMinutes != 0) return false
    if (snoozeUntilMinute != null && usageMinutes < snoozeUntilMinute) return false
    if (lastReminderMinute != null && usageMinutes - lastReminderMinute < intervalMinutes) return false
    return true
}

internal fun isVisualEffectActive(
    forceEnabled: Boolean,
    nightModeEnabled: Boolean,
    currentHour: Int,
    startHour: Int,
    endHour: Int
): Boolean {
    if (forceEnabled) return true
    if (!nightModeEnabled) return false
    return isWithinProtectionWindow(
        currentHour = currentHour,
        startHour = startHour,
        endHour = endHour
    )
}

internal fun resolveEyeVisualState(
    settingsPreviewEnabled: Boolean,
    forceEnabled: Boolean,
    nightModeEnabled: Boolean,
    currentHour: Int,
    startHour: Int,
    endHour: Int,
    brightnessLevel: Float,
    warmFilterStrength: Float
): EyeVisualState {
    val clampedBrightness = brightnessLevel.coerceIn(0.3f, 1.0f)
    val clampedWarmFilter = warmFilterStrength.coerceIn(0f, 0.5f)

    if (settingsPreviewEnabled) {
        return EyeVisualState(
            isActive = true,
            brightnessLevel = clampedBrightness,
            warmFilterStrength = clampedWarmFilter
        )
    }

    val active = isVisualEffectActive(
        forceEnabled = forceEnabled,
        nightModeEnabled = nightModeEnabled,
        currentHour = currentHour,
        startHour = startHour,
        endHour = endHour
    )
    return if (active) {
        EyeVisualState(
            isActive = true,
            brightnessLevel = clampedBrightness,
            warmFilterStrength = clampedWarmFilter
        )
    } else {
        EyeVisualState(
            isActive = false,
            brightnessLevel = 1.0f,
            warmFilterStrength = 0f
        )
    }
}

internal fun tuningForPreset(preset: EyeCarePreset): EyeCareTuning {
    return when (preset) {
        EyeCarePreset.GENTLE -> EyeCareTuning(
            brightnessLevel = 0.88f,
            warmFilterStrength = 0.12f,
            reminderIntervalMinutes = 45
        )
        EyeCarePreset.BALANCED -> EyeCareTuning(
            brightnessLevel = 0.78f,
            warmFilterStrength = 0.22f,
            reminderIntervalMinutes = 30
        )
        EyeCarePreset.FOCUS -> EyeCareTuning(
            brightnessLevel = 0.65f,
            warmFilterStrength = 0.32f,
            reminderIntervalMinutes = 25
        )
        EyeCarePreset.CUSTOM -> EyeCareTuning(
            brightnessLevel = 0.78f,
            warmFilterStrength = 0.22f,
            reminderIntervalMinutes = 30
        )
    }
}

internal fun buildCareReminderMessage(usageMinutes: Int): String {
    val messages = listOf(
        "你已经连续观看 ${usageMinutes} 分钟了，试着看向 6 米外 20 秒。",
        "眼睛已经很努力了，建议起身活动下肩颈，放松 1 分钟。",
        "喝一口温水，眨眨眼，再继续看会更舒服。"
    )
    return messages[usageMinutes % messages.size]
}
