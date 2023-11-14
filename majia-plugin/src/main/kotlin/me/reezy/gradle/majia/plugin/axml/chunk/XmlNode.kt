package me.reezy.gradle.majia.plugin.axml.chunk

import java.io.OutputStream
import java.nio.ByteBuffer

open class XmlNode(chunkType: Short, buffer: ByteBuffer): Chunk(chunkType, buffer) {
    val lineNumber: Int = buffer.int
    val comment: Int = buffer.int

    override fun writeBodyTo(stream: OutputStream) {
         stream.write(buffer(8).putInt(lineNumber).putInt(comment).array())
    }
}