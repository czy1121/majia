package me.reezy.gradle.majia.plugin.axml.chunk

import java.io.OutputStream
import java.nio.ByteBuffer


class XmlStartElement(buffer: ByteBuffer) : XmlNode(TYPE_START_ELEMENT, buffer) {

    val namespaceUri: Int = buffer.int
    val name: Int = buffer.int
    var attributeStart: Short = buffer.short
    var attributeSize: Short = buffer.short
    var attributeCount: Short = buffer.short
    var idIndex: Short = buffer.short
    var classIndex: Short = buffer.short
    var styleIndex: Short = buffer.short
    var attributes: MutableList<Attribute> = ArrayList(attributeCount.toInt())


    init {
        for (i in 0 until attributeCount) {
            attributes.add(Attribute(buffer))
        }
    }


    override fun writeBodyTo(stream: OutputStream) {
        super.writeBodyTo(stream)


        attributeCount = attributes.size.toShort()
        val buffer = buffer(5 * 4)
        buffer.putInt(namespaceUri)
        buffer.putInt(name)
        buffer.putShort(attributeStart)
        buffer.putShort(attributeSize)
        buffer.putShort(attributeCount)
        buffer.putShort(idIndex)
        buffer.putShort(classIndex)
        buffer.putShort(styleIndex)

        stream.write(buffer.array())

        for (attribute in attributes) {
            stream.write(attribute.toByteArray())
        }
    }

    class Attribute(buffer: ByteBuffer) {
        val namespaceUri: Int = buffer.int
        val name: Int = buffer.int
        val rawValue: Int = buffer.int

        val size: Short = buffer.short
        val res0: Int = buffer.get().toInt() and 0xFF
        val type: Int = buffer.get().toInt() and 0xFF
        val data: Int = buffer.int

        fun toByteArray(): ByteArray {
            val buffer = buffer(5 * 4)
            buffer.putInt(namespaceUri)
            buffer.putInt(name)
            buffer.putInt(rawValue)
            buffer.putShort(size)
            buffer.put(res0.toByte())
            buffer.put(type.toByte())
            buffer.putInt(data)
            return buffer.array()
        }
    }
}