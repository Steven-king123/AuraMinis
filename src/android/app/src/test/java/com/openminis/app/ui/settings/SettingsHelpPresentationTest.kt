package com.openminis.app.ui.settings

import com.openminis.app.ProductionSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dev43 source guards; touch dispatch and TalkBack still need device/Compose checks. */
class SettingsHelpPresentationTest {
    private val help = ProductionSources.read("ui/settings/SettingsHelpLabel.kt")
    private val components = ProductionSources.read("ui/settings/SettingsComponents.kt")

    private fun compact(text: String) = text.replace(Regex("\\s+"), " ")
    private fun section(): String {
        val start = components.indexOf("fun SettingsSection(")
        val end = components.indexOf("fun SettingsRow(", start)
        assertTrue("SettingsSection and its legacy row path must exist", start >= 0 && end > start)
        return components.substring(start, end)
    }

    @Test fun helpHasNoSettingCallbacksOrPersistenceAndNetworkDependencies() {
        val signature = help.substringAfter("fun SettingsHelpLabel(").substringBefore(") {")
        assertTrue(signature.contains("title: String"))
        assertTrue(signature.contains("help: String"))
        assertFalse("help must not accept a setting mutation callback", signature.contains("->"))
        listOf("SharedPreferences", "getAppearancePrefs", "AdvancedMemoryStore", "ChatBackgroundStore",
            "ViewModel", "LocalContext", "LaunchedEffect", "DisposableEffect", "rememberCoroutineScope",
            "scope.launch", "onCheckedChange", "updateConfig", "android.content", "okhttp", "retrofit",
            "com.openminis.app.data", "com.openminis.app.memory", "com.openminis.app.tools").forEach {
            assertFalse("help must be presentation-only: $it", help.contains(it))
        }
        val state = Regex("var\\s+(\\w+)\\s+by\\s+remember[^\\n]*mutableStateOf\\(false\\)")
            .find(help)?.groupValues?.get(1)
        assertTrue("dialog visibility must be local and initially closed", state != null)
        val clickBodies = Regex("onClick\\s*=\\s*\\{([^{}]*)}").findAll(help)
            .map { compact(it.groupValues[1]).trim() }.toList()
        assertEquals("opening and closing are the only help button actions", 2, clickBodies.size)
        assertEquals(setOf("$state = true", "$state = false"), clickBodies.toSet())
        assertTrue(Regex("onDismissRequest\\s*=\\s*\\{\\s*$state\\s*=\\s*false\\s*}").containsMatchIn(help))
    }

    @Test fun questionMarkIsAnIndependentAccessible48DpTarget() {
        val iconButton = help.substringAfter("IconButton(").substringBefore("Icon(")
        assertTrue("explicit 48dp target, not an 18dp clickable glyph",
            Regex("\\.size\\(48\\.dp\\)").containsMatchIn(iconButton))
        assertTrue(help.contains("Icons.Outlined.HelpOutline"))
        assertTrue("localized accessible name must include the affected title",
            Regex("contentDescription\\s*=\\s*stringResource\\(R.string.settings_view_help,\\s*title\\)")
                .containsMatchIn(help))
        assertFalse("the heading/row must not steal the adjacent setting's click", help.contains(".clickable("))
        assertFalse(help.contains("mergeDescendants = true"))
        assertFalse(help.contains("clearAndSetSemantics"))
    }

    @Test fun longHelpScrollsAndHasBothExplicitCloseAndDismiss() {
        val dialog = help.substringAfter("AlertDialog(")
        assertTrue("use an AlertDialog rather than inline expanding settings", help.contains("AlertDialog("))
        assertTrue(dialog.contains("onDismissRequest"))
        assertTrue(dialog.contains("Text(title)"))
        assertTrue(dialog.contains("Text(help"))
        assertTrue(dialog.contains(".verticalScroll(rememberScrollState())"))
        assertTrue("bound the scrollable help viewport", dialog.contains("heightIn(max ="))
        assertTrue(dialog.contains("confirmButton"))
        assertTrue(dialog.contains("TextButton(onClick"))
        assertTrue(dialog.contains("R.string.settings_help_close"))
    }

    @Test fun headerHelpIsOptionalAndPreservesTheOriginalPlainHeaderPath() {
        val source = compact(section())
        assertTrue(source.contains("header: String? = null"))
        assertTrue(source.contains("footer: String? = null"))
        assertTrue(source.contains("modifier: Modifier = Modifier"))
        assertTrue(source.contains("headerHelp: String? = null"))
        assertTrue(source.contains("content: @Composable ColumnScope.() -> Unit"))
        assertTrue(source.contains("if (header != null && headerHelp != null)"))
        assertTrue(source.contains("SettingsHelpLabel("))
        assertTrue(source.contains("help = headerHelp"))
        assertTrue("null help retains the pre-Dev43 heading", source.contains("else if (header != null)"))
        val legacy = source.substringAfter("else if (header != null)").substringBefore("// [T-android-settings-section-symmetry]")
        assertTrue(legacy.contains("text = header.uppercaseForDisplay()"))
        assertTrue(legacy.contains("style = MaterialTheme.typography.labelSmall"))
        assertTrue(legacy.contains("letterSpacing = 0.5.sp"))
        assertTrue(legacy.contains("start = 32.dp, end = 32.dp, bottom = 8.dp"))
        assertFalse(legacy.contains("SettingsHelpLabel("))
        assertTrue(source.contains("content = content"))
        assertTrue(source.contains("if (footer != null)"))
        assertTrue(source.contains("text = footer"))
    }
}
