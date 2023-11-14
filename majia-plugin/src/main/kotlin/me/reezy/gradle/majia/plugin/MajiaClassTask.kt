package me.reezy.gradle.majia.plugin;

import com.google.common.collect.Sets
import org.apache.commons.compress.archivers.jar.JarArchiveEntry
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

abstract class MajiaClassTask : DefaultTask() {


    @get:Internal
    abstract var scopes: List<String>

    @get:Internal
    abstract var mappings: List<MappingItem>

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    private val entries = Sets.newConcurrentHashSet<String>()

    private val remapper by lazy { MajiaRemapper(mappings) }

    @TaskAction
    fun taskAction() {

        val ncpu = Runtime.getRuntime().availableProcessors()
        val executor = Executors.newFixedThreadPool(ncpu)
        val creator = ParallelScatterZipCreator(ThreadPoolExecutor(ncpu, ncpu, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue(), Executors.defaultThreadFactory()) { runnable, _ ->
            runnable.run()
        })
        val inputs = (allJars.get() + allDirectories.get()).map { it.asFile }
        try {
            JarArchiveOutputStream(output.asFile.get().touch().outputStream().buffered()).use { jos ->
                inputs.map {
                    executor.submit { transformFile(creator, it) }
                }.forEach {
                    it.get()
                }
                creator.writeTo(jos)
            }
        } finally {
            executor.shutdown()
            executor.awaitTermination(1L, TimeUnit.HOURS)
        }

    }

    private fun transformFile(creator: ParallelScatterZipCreator, file: File) {
        when {
            file.isDirectory -> file.toURI().let { base ->
                file.search { it.extension.lowercase() == "class" }.parallelStream().forEach {
                    transformClass(creator, base.relativize(it.toURI()).path, it.readBytes())
                }
            }

            file.isFile -> when (file.extension.lowercase()) {
                "class" -> transformClass(creator, file.name, file.readBytes())
                "jar" -> ZipFile(file).use { zip ->
                    zip.entries().asSequence().filter { it.name.endsWith(".class") }.forEach { entry ->
                        transformClass(creator, entry.name, zip.getInputStream(entry).readBytes())
                    }
                }

                else -> println("Not transform file ${file.path}")
            }

            else -> throw IOException("Unexpected file: ${file.canonicalPath}")
        }
    }

    private fun transformClass(creator: ParallelScatterZipCreator, name: String, data: ByteArray) {
        if (entries.contains(name)) return
        entries.add(name)

        val jae = JarArchiveEntry(name.transformClassName(mappings))

        jae.method = JarArchiveEntry.DEFLATED

        creator.addArchiveEntry(jae) {
            if (scopes.isEmpty() || scopes.any { name.startsWith(it) }) {
//                        println("transform => $entryName")
                data.transformBytecode(remapper).inputStream()
            } else {
                data.inputStream()
            }
        }
    }

    private fun String.transformClassName(mappings: List<MappingItem>): String {
        for (item in mappings) {
            // aa/bb/cc
            if (startsWith(item.oldDesc)) {
                return replace(item.oldDesc, item.newDesc)
            }
        }
        return this
    }

    private fun ByteArray.transformBytecode(remapper: Remapper): ByteArray {
        val klass = ClassNode()

        val visitor = ClassRemapper(klass, remapper)

        ClassReader(this).accept(visitor, 0)

        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
        klass.accept(writer)
        return writer.toByteArray()
    }


    class MappingItem(val oldName: String, val newName: String) {
        val oldDesc: String = oldName.replace(".", "/")
        val newDesc: String = newName.replace(".", "/")

        val oldSignature: String = "L$oldDesc"
        val newSignature: String = "L$newDesc"
    }

    class MajiaRemapper(private val mapping: List<MappingItem>) : Remapper() {
        override fun map(value: String): String {
            return mapping.relocate(value)
//                .also { if (it != value) println("mapValue($value)) => $it") }
        }

        override fun mapValue(value: Any?): Any {
            if (value is String) {
                return mapping.relocate(value)
//                .also { if (it != value) println("mapValue($value)) => $it") }
            }
            return super.mapValue(value)
        }


        private fun List<MappingItem>.relocate(value: String): String {
            for (item in this) {
                // aa.bb.cc
                if (value.startsWith(item.oldName)) {
                    return value.replace(item.oldName, item.newName)
                }
                // aa/bb/cc
                if (value.startsWith(item.oldDesc)) {
                    return value.replace(item.oldDesc, item.newDesc)
                }
                // Laa/bb/cc;
                if (value.contains(item.oldSignature)) {
                    return value.replace(item.oldSignature, item.newSignature)
                }
            }
            return value
        }
    }
}