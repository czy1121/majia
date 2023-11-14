package me.reezy.gradle.majia.plugin.axml.chunk

import java.io.OutputStream
import java.nio.ByteBuffer


class XmlNamespace(chunkType: Short, buffer: ByteBuffer) : XmlNode(chunkType, buffer) {
    val prefix: Int = buffer.int
    val uri: Int = buffer.int

    override fun writeBodyTo(stream: OutputStream) {
        super.writeBodyTo(stream)
        stream.write(buffer(8).putInt(prefix).putInt(uri).array())
    }
}