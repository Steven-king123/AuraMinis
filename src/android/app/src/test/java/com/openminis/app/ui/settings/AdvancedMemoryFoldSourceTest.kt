package com.openminis.app.ui.settings

import com.openminis.app.ProductionSources
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

/** Structural guards complement policy tests; no claim of instrumented touch/render coverage. */
class AdvancedMemoryFoldSourceTest {
    private val source = ProductionSources.read("ui/settings/AdvancedMemorySettings.kt")
    private val foldedStart = source.indexOf("    if (config.enabled) {")
    private val foldedEnd = source.indexOf("    } // config.enabled", foldedStart)

    @Test fun switchAndOperationScopeSurviveAllAdvancedContentBeingFolded() {
        assertTrue(foldedStart >= 0 && foldedEnd > foldedStart)
        val outside = source.substring(0, foldedStart)
        val inside = source.substring(foldedStart, foldedEnd)
        assertTrue(outside.contains("val scope = rememberCoroutineScope()"))
        assertTrue(outside.contains("R.string.am_enabled"))
        assertTrue(outside.contains("update(requireEnabled = false) { it.copy(enabled = value) }"))
        listOf("am_models", "am_retrieval", "am_automatic", "am_manage", "am_test", "am_maintenance").forEach {
            assertFalse("advanced group outside fold: $it", outside.contains("R.string.$it"))
            assertTrue("missing folded group $it", inside.contains("R.string.$it"))
        }
        listOf("picker?.let", "if (confirmAuto)", "if (confirmReindex)", "if (confirmSummarize)",
            "deleting?.let", "if (editorOpen)").forEach { assertTrue("dialog outside fold: $it", inside.contains(it)) }
    }

    @Test fun disableClearsOnlyTransientDialogsAndKeepsPersistentOptions() {
        val cleanup = source.substringAfter("LaunchedEffect(config.enabled) {").substringBefore("fun runOperation(")
        listOf("picker = null", "confirmAuto = false", "confirmReindex = false", "confirmSummarize = false",
            "editing = null", "editorOpen = false", "deleting = null").forEach { assertTrue(cleanup.contains(it)) }
        listOf("updateConfig", "autoConsolidate = false", "store.delete", "store.save", "summarizerEntryId =",
            "embeddingEntryId =", "threshold =").forEach { assertFalse(cleanup.contains(it)) }
        assertTrue(source.contains("AdvancedMemoryUiPolicy.canStartOperation(store.config.value.enabled, busy, requireEnabled)"))
        assertTrue(source.contains("store.updateIfRevision(original.id, original.revision, content)"))
        assertTrue(source.contains("store.deleteIfRevision(record.id, record.revision)"))
    }

    @Test fun exactlyFiveHelpConceptsAreAvailableWithoutExtraPickerHelp() {
        assertEquals(3, Regex("headerHelp\\s*=").findAll(source).count())
        assertEquals(2, Regex("SettingsHelpLabel\\(").findAll(source).count())
        val picker = source.substringAfter("private fun MemoryModelDialog(")
            .substringBefore("private fun MemoryEditor(")
        assertFalse(picker.contains("am_picker_embedding_priority"))
        assertFalse(picker.contains("SettingsHelpLabel"))
        assertTrue(source.contains("R.string.am_auto_cost"))
        assertTrue(source.contains("R.string.am_query_disclosure"))
        assertTrue(source.contains("AdvancedMemoryUiPolicy.showStatus"))
    }

    @Test fun bilingualModelRequirementsAndFoldHelpAreCurrent() {
        for (locale in listOf("values", "values-zh")) {
            val file = File(requireNotNull(ProductionSources.mainRoot()), "../../../../res/$locale/strings_advanced_memory.xml").canonicalFile
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            val text = (0 until nodes.length).associate {
                nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent
            }
            val model = requireNotNull(text["am_embedding_help"])
            listOf("API", "/embeddings", "OAuth", "Azure", "Anthropic", "Gemini").forEach { assertTrue(model.contains(it)) }
            assertFalse(model.contains("第一版"))
            assertFalse(model.contains("first version", ignoreCase = true))
            val mode = requireNotNull(text["am_mode_help"])
            assertTrue(mode.contains("GLOBAL.md"))
            assertFalse(mode.contains("两种模式下均可管理"))
            assertFalse(requireNotNull(text["am_test_help"]).contains("标准模式下也可测试"))
        }
    }
}
