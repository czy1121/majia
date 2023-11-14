package me.reezy.gradle.majia.plugin.axml

import me.reezy.gradle.majia.plugin.axml.chunk.Chunk
import me.reezy.gradle.majia.plugin.axml.chunk.XmlEndElement
import me.reezy.gradle.majia.plugin.axml.chunk.XmlNamespace
import me.reezy.gradle.majia.plugin.axml.chunk.ResourceMap
import me.reezy.gradle.majia.plugin.axml.chunk.XmlStartElement
import me.reezy.gradle.majia.plugin.axml.chunk.StringPool
import me.reezy.gradle.majia.plugin.axml.chunk.XmlNode
import me.reezy.gradle.majia.plugin.axml.util.TypedValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryXml(bytes: ByteArray) {
    var fileType: Short
    var headerSize: Short
    var fileSize: Int

    var stringPool: StringPool? = null
    var resourceMap: ResourceMap? = null

    var nodeList: MutableList<XmlNode> = ArrayList()
    var namespaceList: MutableList<XmlNamespace> = ArrayList()

    constructor(file: File) : this(file.readBytes())


    init {
        val available = bytes.size
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fileType = buffer.short
        headerSize = buffer.short
        fileSize = buffer.int


        while (buffer.position() < available) {
            when (val chunkType = buffer.short) {
                Chunk.TYPE_STRING_POOL -> stringPool = StringPool(buffer)
                Chunk.TYPE_RESOURCE_MAP -> resourceMap = ResourceMap(buffer)
                Chunk.TYPE_START_NAMESPACE -> {
                    val chunk = XmlNamespace(chunkType, buffer)
                    namespaceList.add(chunk)
                    nodeList.add(chunk)
                }

                Chunk.TYPE_START_ELEMENT -> nodeList.add(XmlStartElement(buffer))
                Chunk.TYPE_END_ELEMENT -> nodeList.add(XmlEndElement(buffer))
                Chunk.TYPE_END_NAMESPACE -> nodeList.add(XmlNamespace(chunkType, buffer))
            }
        }
    }

    fun toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        stringPool?.writeTo(stream)
        resourceMap?.writeTo(stream)
        for (node in nodeList) {
            node.writeTo(stream)
        }
        val fileSize = 8 + stream.size()

        val buffer = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(fileType)
        buffer.putShort(headerSize)
        buffer.putInt(fileSize)
        buffer.put(stream.toByteArray())
        return buffer.array()
    }

    fun toXmlString(): String {
        val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        var isRoot = true
        for (chunk in nodeList) {
            if (chunk is XmlStartElement) {
                sb.append(chunk.toXmlString(isRoot))
                isRoot = false
            } else if (chunk is XmlEndElement) {
                sb.append(chunk.toXmlString())
            }
        }
        return sb.toString()
    }

    private fun getString(index: Int): String {
        return if (index == -1) "" else stringPool?.stringList?.get(index) ?: ""
    }

    private fun getNamespaceList(): String {
        return namespaceList.joinToString(" ") {
            "xmlns:${getString(it.prefix)}=\"${getString(it.uri)}\""
        }
    }

    private fun getNamespacePrefix(uri: Int): String {
        for (chunk in namespaceList) {
            if (chunk.uri == uri) {
                return getString(chunk.prefix)
            }
        }
        return ""
    }

    private fun XmlStartElement.toXmlString(isRoot: Boolean): String {
        val builder = StringBuilder()
        if (comment > -1) builder.append("<!--").append(getString(comment)).append("-->").append("\n")
        builder.append('<').append(getString(name))
        if (isRoot) {
            builder.append(" ").append(getNamespaceList())
        }
        for (attribute in attributes) {
            builder.append(" ")
            if (attribute.namespaceUri != -1) {
                builder.append(getNamespacePrefix(attribute.namespaceUri)).append(":")
            }
            builder.append(getString(attribute.name))
                .append("=")
                .append('"')
                .append(TypedValue.coerceToString(attribute.type, attribute.data) ?: getString(attribute.rawValue))
                .append('"')
        }
        return builder.append(">\n").toString()
    }

    private fun XmlEndElement.toXmlString(): String {
        return "</${getString(name)}>\n"
    }

}