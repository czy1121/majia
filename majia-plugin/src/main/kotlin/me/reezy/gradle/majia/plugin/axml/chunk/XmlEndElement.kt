package me.reezy.gradle.majia.plugin.axml.chunk

import java.io.OutputStream
import java.nio.ByteBuffer

class XmlEndElement(buffer: ByteBuffer) : XmlNode(TYPE_END_ELEMENT, buffer) {
    val namespaceUri: Int = buffer.int
    val name: Int = buffer.int

    override fun writeBodyTo(stream: OutputStream) {
        super.writeBodyTo(stream)
        stream.write(buffer(8).putInt(namespaceUri).putInt(name).array())
    }
}