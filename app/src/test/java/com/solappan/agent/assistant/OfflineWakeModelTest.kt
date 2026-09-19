package com.solappan.agent.assistant

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflineWakeModelTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun archive(path: String): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry(path)); zip.write("model".toByteArray()); zip.closeEntry()
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }
    @Test fun `model extraction preserves bundled directory structure`() {
        val root = temporary.newFolder("model")
        OfflineWakeModel.extract(archive("model/am/final.mdl"), root, "model/")
        assertEquals("model", root.resolve("am/final.mdl").readText())
    }
    @Test fun `archive cannot write outside model directory`() {
        val root = temporary.newFolder("model")
        assertThrows(IllegalArgumentException::class.java) {
            OfflineWakeModel.extract(archive("model/../escape.txt"), root, "model/")
        }
        assertFalse(temporary.root.resolve("escape.txt").exists())
    }
}
