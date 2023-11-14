package me.reezy.gradle.majia.plugin

import me.reezy.gradle.majia.plugin.axml.BinaryXml
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

internal abstract class MajiaXmlTask : DefaultTask() {

//    @get:Internal
//    abstract var resPackageName: String

    @get:Internal
    abstract var ap: File

    @get:Internal
    abstract var mappings: Map<String, String>

    @TaskAction
    fun transform() {
        val excludes = listOf("m3_", "mtrl_", "material_", "design_", "abc_")
        transformAp(ap, excludes, mappings)
    }

    private fun transformAp(file: File, excludes: List<String>, mapping: Map<String, String>) {

        val dest = File.createTempFile("resources-", ".ap_")
        ZipFile(file).use { zip ->
            val ncpu = Runtime.getRuntime().availableProcessors()
            val creator = ParallelScatterZipCreator(ThreadPoolExecutor(ncpu, ncpu, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue(), Executors.defaultThreadFactory()) { runnable, _ ->
                runnable.run()
            })
            val entries = mutableSetOf<String>()
            zip.entries().asIterator().forEach { entry ->
                if (entries.contains(entry.name)) return@forEach

                val zae = ZipArchiveEntry(entry)
                val data = zip.getInputStream(entry).readBytes()
                creator.addArchiveEntry(zae) {

                    if (!entry.isDirectory && canTransform(entry.name, excludes)) {
                        transform(entry.name, data, mapping).inputStream()
//                    } else if (entry.name == "respirces.arsc" && resPackageName.isNotEmpty()) {
//                        updateResourcePackageName(stream.readBytes()).inputStream()
                    } else {
                        data.inputStream()
                    }
                }
                entries.add(entry.name)
            }
            ZipArchiveOutputStream(dest.outputStream()).use(creator::writeTo)
        }

        if (file.delete()) {
            if (!dest.renameTo(file)) {
                dest.copyTo(file, true)
            }
        }
    }

//    private fun updateResourcePackageName(bytes: ByteArray): ByteArray {
//
//        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
//
//        buffer.short
//        buffer.position(buffer.short.toInt())
//
//        while (buffer.position() < bytes.size) {
//            val chunkStart = buffer.position()
//            val chunkType = buffer.short
//            val headerSize = buffer.short
//            val chunkSize = buffer.int
//            if (chunkType.toInt() == 0x0200) {
//                val packageId = buffer.int
//                val nameBytes = ByteArray(256)// { 0 }
//                resPackageName.toByteArray(Charsets.UTF_16LE).copyInto(nameBytes)
//                buffer.put(nameBytes)
//            }
//            buffer.position(chunkStart + chunkSize)
//        }
//
//        return buffer.array()
//
//    }


    private fun transform(entryName: String, data: ByteArray, mapping: Map<String, String>): ByteArray {
        try {
            val xml = BinaryXml(data)

            xml.stringPool?.stringList = xml.stringPool?.stringList?.map { mapping.transformClassName(it) } as MutableList<String>

            val result = xml.toByteArray()

            val old = Base64.getEncoder().encodeToString(data.sha256())
            val new = Base64.getEncoder().encodeToString(result.sha256())

            if (old != new) {
                println("=====>>> [$old, $new] => $entryName")
//                println("=====>>> [$old, $new] => $entryName \nold = ${data.hex()} \nnew = ${result.hex()} \n${xml.toXmlString()}")
            }
            return result
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        return data
    }


    private val CLASS_NAME_PATTERN = Regex("([a-zA-Z_]\\w*\\.)+([a-zA-Z_]\\w*)(\\$[a-zA-Z_-]\\w*)*")

    private fun Map<String, String>.transformClassName(className: String): String {
        if (CLASS_NAME_PATTERN.matches(className)) {
            for ((k, v) in this) {
                if (className.startsWith(k)) {
                    return className.replace(k, v)
                }
            }
        }
        return className
    }

    private fun canTransform(entryName: String, excludes: List<String>): Boolean {
        // entryName == "AndroidManifest.xml" ||
        return (entryName.startsWith("res/layout") && !excludes.any { entryName.contains("/$it") })
    }
}