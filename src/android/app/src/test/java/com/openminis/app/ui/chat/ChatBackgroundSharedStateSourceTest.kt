package com.openminis.app.ui.chat

import com.openminis.app.ProductionSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source guards for composition ownership, not a substitute for Compose runtime tests.
 * Keep observation/IO behind the standalone fallback and distribute the State holder
 * unchanged so every message can subscribe without owning another preference listener.
 */
class ChatBackgroundSharedStateSourceTest {
    private val background = ProductionSources.read("ui/chat/ChatBackground.kt")
    private val backplate = ProductionSources.read("ui/chat/ChatMessageBackplate.kt")

    private fun section(start: String, end: String): String {
        val from = background.indexOf(start)
        assertTrue("missing section: $start", from >= 0)
        val to = background.indexOf(end, from + start.length)
        assertTrue("missing end: $end", to > from)
        return background.substring(from, to)
    }

    @Test fun localIsNullableAndCarriesTheStableHolderNotASnapshot() {
        assertTrue(background.contains(
            "LocalChatBackgroundState = staticCompositionLocalOf<State<ChatBackgroundState>?> { null }",
        ))
        val provider = section("fun ChatBackgroundStateProvider(", "fun rememberChatBackgroundState()")
        assertTrue(provider.contains("state: State<ChatBackgroundState>"))
        assertTrue(provider.contains("content: @Composable () -> Unit"))
        assertTrue(provider.contains("CompositionLocalProvider(LocalChatBackgroundState provides state)"))
        assertTrue(provider.contains("content()"))
        assertFalse("provider must not subscribe to every value update", provider.contains("state.value"))
        assertFalse("provider must not own observation", provider.contains("rememberOwnedChatBackgroundState("))
        assertFalse("no high-frequency keyboard state in the local provider", provider.contains("WindowInsets"))
    }

    @Test fun sharedPathReturnsBeforeAnyOwnedObservation() {
        val lookup = section("fun rememberChatBackgroundState()", "private fun rememberOwnedChatBackgroundState()")
        val read = lookup.indexOf("val sharedState = LocalChatBackgroundState.current")
        val reuse = lookup.indexOf("if (sharedState != null) return sharedState")
        val fallback = lookup.indexOf("return rememberOwnedChatBackgroundState()")
        assertTrue(read >= 0 && reuse > read && fallback > reuse)
        listOf("LocalContext.current", "getAppearancePrefs(", "DisposableEffect(",
            "LaunchedEffect(", "state.value", "rememberChatBackgroundState()\n").forEach {
            assertFalse("shared path must not perform or copy $it", lookup.contains(it))
        }
    }

    @Test fun onlyTheOwnedFallbackRegistersAndDisposesOneListener() {
        val owned = section("private fun rememberOwnedChatBackgroundState()", "fun ChatBackgroundLayer(")
        assertEquals(1, Regex("prefs\\.registerOnSharedPreferenceChangeListener\\(listener\\)").findAll(background).count())
        assertEquals(1, Regex("prefs\\.unregisterOnSharedPreferenceChangeListener\\(listener\\)").findAll(background).count())
        val registration = owned.indexOf("prefs.registerOnSharedPreferenceChangeListener(listener)")
        assertTrue(registration >= 0)
        val refresh = owned.substring(registration)
        assertTrue(refresh.contains("filename = prefs.getString(ChatBackgroundPolicy.FILE_KEY, null)"))
        assertTrue(refresh.contains("refresh++"))
        assertTrue(refresh.contains("onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }"))
        assertFalse("owned observation must not recurse through local lookup", owned.contains("rememberChatBackgroundState("))
    }

    @Test fun fileValidationRemainsCancellableIoAndUsesTheRefreshKey() {
        val owned = section("private fun rememberOwnedChatBackgroundState()", "fun ChatBackgroundLayer(")
        assertTrue(owned.contains("LaunchedEffect(prefs, filename, refresh)"))
        assertTrue(owned.contains("val checked = withContext(Dispatchers.IO) {\n" +
            "            ChatBackgroundStore.file(context, filename)?.takeIf { it.isFile && it.canRead() }\n" +
            "        }"))
        assertTrue("IO result must preserve more recent appearance settings",
            owned.contains("state.value = state.value.copy(file = checked)"))
        assertEquals(1, Regex("ChatBackgroundStore\\.file\\(").findAll(background).count())
    }

    @Test fun fileRemovalClearAndEveryAppearancePreferenceStillPropagate() {
        val owned = section("private fun rememberOwnedChatBackgroundState()", "fun ChatBackgroundLayer(")
        listOf("FILE_KEY", "DIM_KEY", "MESSAGE_OPACITY_KEY", "GLASS_ENABLED_KEY", "RESIZE_WITH_INPUT_KEY").forEach {
            assertTrue("clear() and $it must refresh the shared state",
                owned.contains("key == null || key == ChatBackgroundPolicy.$it"))
        }
        assertTrue("removal must deactivate all backplates immediately",
            owned.contains("if (filename == null) state.value = state.value.copy(file = null)"))
        assertTrue(owned.contains("state.value.copy(dim = readDim())"))
        assertTrue(owned.contains("state.value.copy(messageOpacity = readMessageOpacity())"))
        assertTrue(owned.contains("state.value.copy(glassEnabled = readGlassEnabled())"))
        assertTrue(owned.contains("state.value.copy(resizeWithInput = readResizeWithInput())"))
    }

    @Test fun wallpaperAndMessageConsumersReadValueAtTheirOwnCompositionScope() {
        val layer = section("fun ChatBackgroundLayer(", "private fun Wallpaper(")
        assertTrue(layer.contains("val state by rememberChatBackgroundState()"))
        assertTrue(layer.contains("Wallpaper(state.file, state.dim, modifier)"))
        assertTrue(backplate.contains("val state by rememberChatBackgroundState()"))
        assertTrue(backplate.contains("hasImage = state.hasImage"))
        assertTrue(backplate.contains("opacity = state.messageOpacity"))
        assertFalse(backplate.contains("getAppearancePrefs("))
        assertFalse(backplate.contains("ChatBackgroundStore.file("))
    }

    @Test fun standaloneSettingsKeepObservationAndPreviewKeepsExplicitDrafts() {
        val settings = section("fun ChatBackgroundSettings()", "private fun ChatAppearancePreview(")
        assertTrue(settings.contains("val state by rememberChatBackgroundState()"))
        assertTrue(settings.contains("ChatAppearancePreview(state, draftDim, draftMessageOpacity)"))
        val preview = background.substring(background.indexOf("private fun ChatAppearancePreview("))
        assertTrue(preview.contains("state: ChatBackgroundState, dim: Float, messageOpacity: Float"))
        assertTrue(preview.contains("Wallpaper(state.file, dim, Modifier.fillMaxSize())"))
        assertTrue(preview.contains("MaterialTheme.colorScheme.background.luminance() < 0.5f, messageOpacity)"))
        assertFalse("preview must not own another observer", preview.contains("rememberChatBackgroundState("))
    }
}
