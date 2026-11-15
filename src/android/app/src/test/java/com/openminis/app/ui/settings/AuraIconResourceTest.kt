package com.openminis.app.ui.settings

import com.openminis.app.ProductionSources
import java.io.File
import java.nio.ByteBuffer
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class AuraIconResourceTest {
    private val res = File(requireNotNull(ProductionSources.mainRoot()), "../../../../res").canonicalFile
    @Test fun forcedVariantsNeverResolveAutoOrOppositeThemeArtwork() {
        for (dir in listOf("mipmap-anydpi-v26", "mipmap-night-anydpi-v26")) {
            for (theme in listOf("light", "dark")) {
                val text = File(res,"$dir/ic_launcher_classic_$theme.xml").readText()
                assertTrue(text.contains("@mipmap/ic_launcher_foreground_$theme"))
                assertTrue(text.contains("@drawable/ic_launcher_background_$theme"))
                assertTrue(text.contains("@drawable/ic_launcher_monochrome"))
                assertTrue(text.contains("android:inset=\"0%\""))
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res,"$dir/ic_launcher_classic_$theme.xml"))
            }
        }
        assertTrue(File(res,"mipmap-anydpi-v26/ic_launcher.xml").readText().contains("background_light"))
        assertTrue(File(res,"mipmap-night-anydpi-v26/ic_launcher.xml").readText().contains("background_dark"))
    }
    @Test fun transparentForegroundContainsOnlyApprovedMarkInsideSafeCircle() {
        val densities = mapOf("mdpi" to 108,"hdpi" to 162,"xhdpi" to 216,"xxhdpi" to 324,"xxxhdpi" to 432)
        for ((density, size) in densities) {
            for (theme in listOf("light", "dark")) {
                val bytes = File(res,"mipmap-$density/ic_launcher_foreground_$theme.png").readBytes()
                assertTrue(bytes.copyOfRange(0,8).contentEquals(byteArrayOf(-119,80,78,71,13,10,26,10)))
                val header = ByteBuffer.wrap(bytes)
                assertEquals(size,header.getInt(16)); assertEquals(size,header.getInt(20))
                assertEquals("RGBA foreground required",6,bytes[25].toInt())
            }
            // Per-pixel alpha parity/safe-circle checks run in tools/test_icons.py.
            assertTrue(File(res,"mipmap-$density/ic_launcher_foreground.png").readBytes().contentEquals(
                File(res,"mipmap-$density/ic_launcher_foreground_light.png").readBytes()))
            assertTrue(File(res,"mipmap-night-$density/ic_launcher_foreground.png").readBytes().contentEquals(
                File(res,"mipmap-$density/ic_launcher_foreground_dark.png").readBytes()))
        }
    }
    @Test fun monochromeIsOriginalASilhouetteWithTransparentCounter() {
        val xml=File(res,"drawable/ic_launcher_monochrome.xml")
        val doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml)
        val node=doc.getElementsByTagName("path").item(0).attributes
        assertEquals("evenOdd",node.getNamedItem("android:fillType").nodeValue)
        assertEquals("#FFFFFFFF",node.getNamedItem("android:fillColor").nodeValue)
        val path=node.getNamedItem("android:pathData").nodeValue
        assertEquals(2,Regex("M").findAll(path).count())
        assertTrue(path.contains("C"))
    }
}
