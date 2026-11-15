package com.openminis.app.ui.chat

import com.openminis.app.ProductionSources
import org.junit.Assert.*
import org.junit.Test

/** Source topology guards: these controls intentionally rely on the composer's glass parent.
 * Unlike an effect-count-only test, the call-site scan fails for reuse outside that parent.
 * No Compose/device dependencies: actual rendering/quality still needs on-device validation.
 */
class ChatComposerGlassOwnershipTest {
    private val chatPath = "ui/chat/ChatScreen.kt"
    private val widgetPath = "ui/chat/ChatComposerWidgets.kt"
    private fun code(path: String) = maskNonCode(ProductionSources.read(path))

    @Test fun everyControlUseIsInsideTheLiveComposerCard() {
        val screen = code(chatPath)
        val card = composerBody(screen)
        val uses = mutableListOf<String>()
        ProductionSources.allKotlinFiles().forEach { file ->
            val source = maskNonCode(file.readText())
            Regex("\\b(InputCircleButton|MicButton)\\b").findAll(source).forEach { match ->
                val definition = Regex("\\bfun\\s*$").containsMatchIn(source.substring(0, match.range.first))
                if (definition) {
                    assertEquals("Definitions must stay composer-local", "ChatComposerWidgets.kt", file.name)
                } else {
                    // Includes calls, callable references, imports/aliases: any new reuse requires review.
                    assertEquals("Standalone reuse needs an explicit glass/fallback decision", "ChatScreen.kt", file.name)
                    assertTrue("${match.value} escaped the glass composer", match.range.first in card)
                    uses += match.value
                }
            }
        }
        assertEquals(listOf("InputCircleButton", "InputCircleButton", "MicButton"), uses)
    }

    @Test fun childButtonsKeepFillBorderAndGesturesButNoEffect() {
        val widgets = code(widgetPath)
        val circle = functionBody(widgets, "InputCircleButton")
        val mic = functionBody(widgets, "MicButton")
        for (body in listOf(circle, mic)) {
            assertFalse("No duplicate background or foreground blur in composer controls",
                Regex("\\b(chatGlassSurface|chatGlass|hazeEffect|blur)\\s*\\(").containsMatchIn(body))
            assertTrue(body.contains(".size(38.dp)"))
            assertTrue(body.contains(".clip(CircleShape)"))
        }
        assertTrue(circle.contains(".background(ChatColors.inputIconBg, CircleShape)"))
        assertTrue(circle.contains(".border(0.5.dp, ChatColors.inputIconBorder, CircleShape)"))
        assertTrue(circle.contains(".clickable(onClick = onClick)"))
        assertTrue(mic.contains("if (isRecording) Color.Red.copy(alpha = 0.15f)"))
        assertTrue(mic.contains("if (isRecording) Color.Transparent else ChatColors.inputIconBorder"))
        assertTrue(mic.contains(".background(bg, CircleShape)"))
        assertTrue(mic.contains(".border(0.5.dp, borderColor, CircleShape)"))
        assertTrue(mic.contains(".combinedClickable("))
        assertTrue(mic.contains("onClick = onClick"))
        assertTrue(mic.contains("onLongClick = onLongClick"))
        assertTrue(mic.contains("if (isVoiceActive) Icons.Default.Keyboard else Icons.Default.Mic"))
        assertTrue(mic.contains("if (!localeBadge.isNullOrEmpty())"))
    }

    @Test fun independentToolbarAndTopBarKeepTheirOwnEffect() {
        val widgets = code(widgetPath)
        val toolbar = functionBody(widgets, "FloatingToolStatusBar")
        assertEquals(1, Regex("\\.chatGlassSurface\\s*\\(").findAll(widgets).count())
        assertTrue(toolbar.contains(".chatGlassSurface(shape = RoundedCornerShape(10.dp), fallback = ChatColors.inputBg)"))
        val screen = code(chatPath)
        assertEquals(2, Regex("\\.chatGlassSurface\\s*\\(").findAll(screen).count())
        assertTrue(screen.contains("Modifier.chatGlassSurface(RoundedCornerShape(0.dp), ChatColors.background)"))
        composerBody(screen) // also requires the separate composer effect
    }

    @Test fun libraryScaleIsExplicitWithoutChangingStyleOrGatingOnKeyboard() {
        val glass = code("ui/chat/ChatGlass.kt")
        val surface = functionBody(glass, "chatGlassSurface")
        assertTrue(surface.contains(".hazeEffect(state = glass.hazeState, style = style)"))
        assertTrue(surface.contains("inputScale = HazeInputScale.Fixed(0.5f)"))
        assertEquals(1, Regex("\\bhazeEffect\\s*\\(").findAll(glass).count())
        assertTrue(surface.contains("blurRadius = 24.dp"))
        assertTrue(surface.contains("noiseFactor = 0.02f"))
        assertTrue(surface.contains("if (!glass.enabled) return background(fallback, shape)"))
        assertTrue(surface.contains("Build.VERSION.SDK_INT < Build.VERSION_CODES.S || glass.hazeState == null"))
        assertTrue(surface.contains("return clip(shape).background(safeFallback)"))
        assertFalse(Regex("(?i)\\b(ime|keyboard|inputFocused|isImeVisible|blurEnabled|clipToAreasBounds)\\b").containsMatchIn(glass))
        assertTrue(glass.contains("ChatGlassContext(enabled, state)"))
    }

    @Test fun wallpaperAndMessagesRemainSeparateLiveSources() {
        val screen = code(chatPath)
        assertTrue(screen.contains("ChatBackgroundLayer(Modifier.matchParentSize().chatGlassSource())"))
        assertTrue(screen.contains(".chatGlassSource(zIndex = 1f)"))
        assertEquals(2, Regex("\\.chatGlassSource\\s*\\(").findAll(screen).count())
        val glass = code("ui/chat/ChatGlass.kt")
        assertTrue(glass.contains("hazeSource(state = glass.hazeState, zIndex = zIndex)"))
    }

    private fun composerBody(source: String): IntRange {
        val candidates = Regex("\\bColumn\\s*\\(").findAll(source).mapNotNull { match ->
            val open = source.indexOf('(', match.range.first)
            val close = matching(source, open, '(', ')')
            val args = source.substring(open, close)
            if (!args.contains(".chatGlassSurface(RoundedCornerShape(20.dp), Color.Transparent)")) null
            else bodyAfter(source, close + 1)
        }.toList()
        assertEquals("Exactly one live glass composer parent required", 1, candidates.size)
        return candidates.single()
    }

    private fun functionBody(source: String, name: String): String {
        val match = Regex("\\bfun\\s+(?:Modifier\\.)?${name}\\s*\\(").find(source)
            ?: error("Missing function $name")
        val open = source.indexOf('(', match.range.first)
        val close = matching(source, open, '(', ')')
        // Optional return type between parameter list and opening brace.
        val start = source.indexOf('{', close + 1)
        return source.substring(start + 1, matching(source, start, '{', '}'))
    }

    private fun bodyAfter(source: String, offset: Int): IntRange {
        val start = (offset until source.length).first { !source[it].isWhitespace() }
        check(source[start] == '{') { "Expected composable content lambda" }
        return (start + 1) until matching(source, start, '{', '}')
    }

    private fun matching(source: String, start: Int, open: Char, close: Char): Int {
        var depth = 0
        for (i in start until source.length) {
            if (source[i] == open) depth++
            if (source[i] == close && --depth == 0) return i
        }
        error("Unbalanced $open at $start")
    }

    /** Preserve offsets while ignoring strings/comments, including nested Kotlin block comments. */
    private fun maskNonCode(source: String): String {
        val result = source.toCharArray()
        var i = 0
        fun erase(start: Int, end: Int) {
            for (j in start until end) if (result[j] != '\n' && result[j] != '\r') result[j] = ' '
        }
        while (i < source.length) {
            val start = i
            when {
                source.startsWith("//", i) -> {
                    while (i < source.length && source[i] != '\n') i++
                }
                source.startsWith("/*", i) -> {
                    i += 2
                    var depth = 1
                    while (i < source.length && depth > 0) {
                        when {
                            source.startsWith("/*", i) -> { depth++; i += 2 }
                            source.startsWith("*/", i) -> { depth--; i += 2 }
                            else -> i++
                        }
                    }
                }
                source.startsWith("\"\"\"", i) -> {
                    val end = source.indexOf("\"\"\"", i + 3)
                    i = if (end < 0) source.length else end + 3
                }
                source[i] == '"' || source[i] == '\'' -> {
                    val quote = source[i++]
                    while (i < source.length) {
                        val c = source[i++]
                        if (c == '\\' && i < source.length) i++
                        else if (c == quote) break
                    }
                }
                else -> { i++; continue }
            }
            erase(start, i)
        }
        return String(result)
    }
}
