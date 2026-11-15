package com.openminis.app.ui.settings

import com.openminis.app.ProductionSources
import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-only Dev43 copy guards. No Android runtime, network or Dev42 checkout required. */
class AppearanceCopy43Test {
    private val background = ProductionSources.read("ui/chat/ChatBackground.kt")
    private fun between(start: String, end: String): String {
        val from = background.indexOf(start)
        val to = background.indexOf(end, from + start.length)
        assertTrue("missing source boundary $start / $end", from >= 0 && to > from)
        return background.substring(from, to)
    }
    private val settings get() = between("fun ChatBackgroundSettings()", "private fun ChatAppearancePreview(")
    private fun strings(qualifier: String): Map<String, String> {
        val dir = File(requireNotNull(ProductionSources.mainRoot()), "../../../../res/$qualifier").canonicalFile
        assertTrue("missing $qualifier resources", dir.isDirectory)
        val result = linkedMapOf<String, String>()
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        dir.listFiles()!!.filter { it.extension == "xml" }.sortedBy { it.name }.forEach { file ->
            val entries = parser.parse(file).getElementsByTagName("string")
            for (i in 0 until entries.length) {
                val entry = entries.item(i)
                result[entry.attributes.getNamedItem("name").nodeValue] = entry.textContent.trim()
            }
        }
        return result
    }
    private fun text(map: Map<String, String>, key: String) = requireNotNull(map[key]) { "missing string $key" }
    private fun digest(source: String): String = MessageDigest.getInstance("SHA-256")
        .digest(source.replace("\r\n", "\n").toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    @Test fun appearanceHasNoQuestionMarksOrNewHelpEntrypoints() {
        listOf("SettingsHelpLabel(", "headerHelp =", "HelpOutline", "HelpCenter", "AlertDialog(").forEach {
            assertFalse("appearance must not gain a help entry: $it", settings.contains(it))
        }
        val keys = Regex("R\\.string\\.(\\w+)").findAll(settings).map { it.groupValues[1] }.toSet()
        for (locale in listOf("values", "values-zh")) {
            val copy = strings(locale)
            keys.forEach { key ->
                val value = text(copy, key)
                assertFalse("$locale/$key must not contain a question mark", value.contains('?') || value.contains('？'))
            }
        }
        assertFalse("glass has no permanent subtitle", settings.contains("R.string.chat_glass_help"))
        assertFalse("opacity has no permanent long explanation", settings.contains("R.string.chat_message_opacity_help"))
        assertTrue(settings.contains("R.string.chat_background_resize_help"))
    }

    @Test fun bilingualLabelsFooterAndChooseImageHintStayShort() {
        for (locale in listOf("values", "values-zh")) {
            val copy = strings(locale)
            val zh = locale == "values-zh"
            assertTrue(text(copy, "chat_glass_title").length <= if (zh) 8 else 28)
            assertTrue(text(copy, "chat_background_resize_title").length <= if (zh) 10 else 40)
            assertTrue(text(copy, "chat_background_resize_help").length <= if (zh) 30 else 110)
            val opacity = text(copy, "chat_message_opacity")
            assertTrue("keep the numeric percentage", opacity.contains("%1\$d%%"))
            assertTrue(opacity.length <= if (zh) 24 else 55)
            assertTrue("overlay percentage must not change", text(copy, "chat_background_dim").contains("%1\$d%%"))
            val footer = text(copy, "chat_background_footer")
            assertTrue("footer contains only concise scope/privacy copy", footer.length <= if (zh) 70 else 160)
            assertFalse("size belongs by the picker, not the footer", footer.contains("20"))
            if (zh) {
                assertEquals("玻璃效果", text(copy, "chat_glass_title"))
                assertEquals("背景自动缩放", text(copy, "chat_background_resize_title"))
                assertTrue(opacity.contains("消息底色"))
                assertTrue(footer.contains("所有聊天") && footer.contains("本机") && footer.contains("模型"))
                assertTrue(footer.contains("不会") || footer.contains("不发送"))
            } else {
                val lower = footer.lowercase()
                assertTrue(lower.contains("all chats") && lower.contains("device") && lower.contains("model"))
                assertTrue(lower.contains("not sent") || lower.contains("never sent"))
            }
            // The implementation may choose its own new resource name for this short hint.
            val choose = settings.substringAfter("R.string.chat_background_choose")
                .substringBefore("R.string.chat_background_remove")
            val chooseCopy = Regex("R\\.string\\.(\\w+)").findAll(choose)
                .map { text(copy, it.groupValues[1]) }.toList()
            assertTrue("$locale: picker row needs a short 20 MB limit hint",
                chooseCopy.any { Regex("20\\s*MB", RegexOption.IGNORE_CASE).containsMatchIn(it) && it.length <= 65 })
        }
    }

    @Test fun copyChangesPreserveSettingsActionsStatusErrorsAndPreview() {
        listOf("val state by rememberChatBackgroundState()", "ChatBackgroundStore.busy.collectAsState()",
            "ActivityResultContracts.GetContent()", "ChatBackgroundStore.importImage(application, uri)",
            "ChatBackgroundStore.setGlassEnabled(application, value)",
            "ChatBackgroundStore.setResizeWithInput(application, value)",
            "ChatBackgroundStore.remove(application)", "ChatBackgroundStore.setDim(application, value)",
            "ChatBackgroundStore.setMessageOpacity(application, value)",
            "R.string.chat_background_working", "R.string.chat_background_selected", "R.string.chat_background_none",
            "R.string.chat_background_error_generic", "R.string.chat_background_error_picker",
            "ChatAppearancePreview(state, draftDim, draftMessageOpacity)", "valueRange = 0f..1f").forEach {
            assertTrue("copy-only change must preserve $it", settings.contains(it))
        }
        val imageOnly = settings.indexOf("if (state.hasImage)")
        val opacity = settings.indexOf("R.string.chat_message_opacity,")
        assertTrue("opacity remains image-dependent", imageOnly >= 0 && opacity > imageOnly)
        assertTrue(settings.contains("catch (cancelled: CancellationException)"))
        assertTrue(settings.contains("throw cancelled"))
    }

    @Test fun dev42WallpaperSharedStateAndPreviewRemainByteStable() {
        // Baselines read from the completed local minis-dev-42 tree; embedded so CI needs no sibling checkout.
        assertEquals("state ownership, IO and Wallpaper are outside the Dev43 copy scope",
            "bc9a0d4f3ea23466d3b5ed665bd75c02e6299596a85b0160d159c69a57a9f171",
            digest(between("data class ChatBackgroundState(", "/** Drop directly")))
        assertEquals("preview rendering must not change during copy simplification",
            "2b4e9fa20be349d3bd430a55e81944aa27697f15456e320df80f4a13cae1e7ba",
            digest(background.substring(background.indexOf("private fun ChatAppearancePreview("))))
        mapOf(
            "ui/chat/ChatBackgroundStore.kt" to "deed11800300cac8ba424b492fae158bfb135c58ef39d7b80fe8a88a14033286",
            "ui/chat/ChatGlass.kt" to "0e5d8d04d5e54225d11d52e993e75a7d1d26694f029f01a3dcc5589cb358cbbf",
            "ui/chat/ChatMessageBackplate.kt" to "53c359af26bd6f0ce8cbf2b9d213aa8dd7f78e76427e3ef31f386c28e1c9b810",
        ).forEach { (path, expected) -> assertEquals("Dev42 render/storage core: $path", expected, digest(ProductionSources.read(path))) }
    }

    @Test fun dev42MemoryAlgorithmsDefaultsApiSupportAndPersistenceAreUntouched() {
        mapOf(
            "Models" to "c8448a9098257161767f1de66e91dbd6e21ffdad715ca973ec5417893a6e303f",
            "Policy" to "4ce85e7eb07fee3942a0d59df76740a46c670740e75ed238ab5ac7c6c4e5ddb1",
            "Store" to "bb55da6a1dd1ff823486563f61d2bb971dfa47979bdadaa85498b076052decc9",
            "Runtime" to "a6164f9bda795a2b69783c4f3db8b617299d899ff153285142a73e83476285b3",
            "IdleScheduler" to "2023e31bc36750e4c6b99c70c64f6482a49b3cae06f6acc6864eebf5bb28e8bc",
            "Tools" to "7207ddba89f133d84b18bc401c3e71d01fbf14bac5c18fe1172383fcc6b0b7ef",
            "Prompt" to "f08a6b62855262a41812f9c76679e8b8ae10bd992a1a851e8b4902d8af371568",
        ).forEach { (name, expected) ->
            val current = ProductionSources.read("memory/AdvancedMemory$name.kt")
            // AuraMinis isolates only the alarm action under its applicationId.
            // Normalize that reviewed identity-only change; retain the algorithm hash guard.
            val comparable = if (name == "IdleScheduler") current.replace(
                "private const val ACTION = com.openminis.app.BuildConfig.APPLICATION_ID + \".memory.IDLE\"",
                "private const val ACTION = \"com.openminis.app.memory.IDLE\"",
            ) else current
            assertEquals("Dev43 algorithm baseline (except isolated alarm identity): $name", expected,
                digest(comparable))
        }
    }
}
