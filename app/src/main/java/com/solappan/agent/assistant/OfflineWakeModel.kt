package com.solappan.agent.assistant

import android.content.Context
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

internal object OfflineWakeModel {
    private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
    fun install(context: Context): File {
        val directory = File(context.filesDir, MODEL_NAME)
        val marker = File(directory, ".complete")
        if (marker.isFile && File(directory, "am/final.mdl").isFile && File(directory, "conf/model.conf").isFile) return directory
        context.assets.open("$MODEL_NAME.zip").use { extract(it, directory, "$MODEL_NAME/") }
        check(File(directory, "am/final.mdl").isFile && File(directory, "conf/model.conf").isFile) { "Incomplete voice model" }
        marker.writeText("1")
        return directory
    }
    internal fun extract(input: InputStream, directory: File, prefix: String) {
        check(directory.isDirectory || directory.mkdirs())
        val root = directory.canonicalFile
        var total = 0L
        var count = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++count <= 1_000 && entry.name.startsWith(prefix)) { "Invalid model archive" }
                val target = File(root, entry.name.removePrefix(prefix)).canonicalFile
                require(target == root || target.path.startsWith(root.path + File.separator)) { "Invalid model archive path" }
                if (entry.isDirectory) check(target.isDirectory || target.mkdirs())
                else {
                    require(target != root)
                    check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                    target.outputStream().use { output ->
                        val buffer = ByteArray(16_384)
                        while (true) {
                            if (Thread.currentThread().isInterrupted) throw InterruptedException("Model installation cancelled")
                            val read = zip.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= 250_000_000) { "Model archive too large" }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }
}
