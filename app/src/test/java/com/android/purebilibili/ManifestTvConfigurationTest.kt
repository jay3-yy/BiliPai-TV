package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ManifestTvConfigurationTest {

    @Test
    fun manifest_declares_leanback_features_for_tv() {
        val manifest = loadManifestText()

        assertTrue(
            manifest.contains("""android.software.leanback"""),
            "AndroidManifest must declare android.software.leanback feature"
        )
        assertTrue(
            manifest.contains("""android.hardware.touchscreen""") &&
                manifest.contains("""android:required="false""""),
            "AndroidManifest should mark touchscreen as not required for TV devices"
        )
    }

    @Test
    fun manifest_exposes_leanback_launcher_and_banner() {
        val manifest = loadManifestText()

        assertTrue(
            manifest.contains("""android.intent.category.LEANBACK_LAUNCHER"""),
            "AndroidManifest must expose LEANBACK_LAUNCHER entry for TV home"
        )
        assertTrue(
            manifest.contains("""android:banner="""),
            "AndroidManifest application should define banner for TV launcher"
        )
    }

    private fun loadManifestText(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        val manifestFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate AndroidManifest.xml from ${File(".").absolutePath}")
        return manifestFile.readText()
    }
}
