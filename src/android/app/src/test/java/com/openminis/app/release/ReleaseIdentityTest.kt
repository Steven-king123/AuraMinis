package com.openminis.app.release

import com.openminis.app.BuildConfig
import com.openminis.app.data.repository.AppIconRepository
import com.openminis.app.scheduled.ScheduledTaskManager
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Works on debug and release test variants; release-only assertions stay gated. */
class ReleaseIdentityTest {
    @Test fun independentIdentityAndUpdateIsolation() {
        assertEquals("io.github.stevenking123.auraminis", BuildConfig.APPLICATION_ID)
        assertEquals(44, BuildConfig.VERSION_CODE)
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
        assertFalse(BuildConfig.SELF_UPDATE)
        assertFalse(BuildConfig.AUTO_UPDATE_CHECK)
        assertFalse(BuildConfig.IN_APP_INSTALL)
        assertEquals("Steven-king123/AuraMinis", BuildConfig.RELEASE_REPO)
        if (BuildConfig.BUILD_TYPE == "release") {
            assertFalse(BuildConfig.DEBUG)
            assertFalse(BuildConfig.DEV_TOOLS)
        }
    }

    @Test fun forkFeedbackDoesNotTargetUpstreamMaintainers() {
        val settings = File("src/main/java/com/openminis/app/ui/settings/SettingsScreen.kt").readText()
        assertTrue(settings.contains("https://github.com/Steven-king123/AuraMinis/issues/new"))
        assertFalse(settings.contains("dev@openminis.app"))
        assertFalse(settings.contains("https://t.me/+2NzhOJuzRyI1YmM1"))
        val crashes = File("src/main/java/com/openminis/app/crash/CrashFrequencyDetector.kt").readText()
        assertFalse(crashes.contains("dev@openminis.app"))
        assertTrue(crashes.contains("if (recipient.isNotBlank())"))
    }

    @Test fun aliasNamesRemainInTheRealClassNamespace() {
        assertEquals(setOf(
            "com.openminis.app.MainActivityIconAuto",
            "com.openminis.app.MainActivityIconLight",
            "com.openminis.app.MainActivityIconDark",
        ), AppIconRepository.Variant.entries.map { it.aliasClass }.toSet())
        assertEquals(BuildConfig.APPLICATION_ID + ".scheduled.FIRE", ScheduledTaskManager.ACTION_FIRE)
    }

    @Test fun staticShortcutsTargetTheInstalledPackageButTheOriginalClass() {
        val xml = File("src/main/res/xml/shortcuts.xml").readText()
        assertEquals(3, Regex("android:targetPackage=\"${Regex.escape(BuildConfig.APPLICATION_ID)}\"").findAll(xml).count())
        assertEquals(3, Regex("android:targetClass=\"com.openminis.app.MainActivity\"").findAll(xml).count())
        assertFalse(xml.contains("android:targetPackage=\"com.openminis.app\""))
    }
}
