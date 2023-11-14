package me.reezy.gradle.majia.plugin

import com.didiglobal.booster.aapt2.Aapt2Container
import com.didiglobal.booster.aapt2.BinaryParser
import com.didiglobal.booster.aapt2.Resources
import com.didiglobal.booster.aapt2.parseAapt2Container
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object XmlFlat {

    fun processDir(file: File, mapping: Map<String, String>) {
        if (file.name.startsWith("layout_") && file.name.endsWith(".xml.flat")) {
            transform(file, mapping)
        }
    }

    fun transform(file: File, mapping: Map<String, String>) {
        transform(file.readBytes(), mapping)?.let {
            file.outputStream().write(it)
        }
    }

    private fun transform(input: ByteArray, mapping: Map<String, String>): ByteArray? {

        val container = BinaryParser(input).use { it.parseAapt2Container() }

        val entry = container.getXmlEntry() ?: return null

        return mapping.replaceNodeName(entry.root)?.toByteArray(container.header, entry)
    }

    private fun Aapt2Container.getXmlEntry(): Aapt2Container.Xml? {
        if (header.count == 1 && entries[0] is Aapt2Container.Xml) {
            return entries[0] as Aapt2Container.Xml
        }
        return null
    }

    private fun Resources.XmlNode.toByteArray(header: Aapt2Container.Header, entry: Aapt2Container.Xml): ByteArray {

        val head = entry.data.toByteArray()
        val data = toByteArray()
        val headPadding = ((4 - head.size % 4) % 4)
        val dataPadding = ((4 - data.size % 4) % 4)


        val output = ByteArrayOutputStream()
        output.use { os ->
            os.write(buffer(4).putInt(header.magic).array())
            os.write(buffer(4).putInt(header.version).array())
            os.write(buffer(4).putInt(header.count).array())

            // entry_type, entry_length
            os.write(buffer(4).putInt(entry.type).array())
            os.write(buffer(8).putLong(head.size + headPadding + data.size + dataPadding.toLong()).array())

            // entry_head_size, entry_data_size
            os.write(buffer(4).putInt(head.size).array())
            os.write(buffer(8).putLong(data.size.toLong()).array())

            // entry_head
            os.write(buffer(head.size).put(head).array())
            if (headPadding > 0) {
                os.write(byteArrayOf(0, 0, 0, 0), 0, headPadding)
            }

            // entry_data
            os.write(buffer(data.size).put(data).array())
            if (dataPadding > 0) {
                os.write(byteArrayOf(0, 0, 0, 0), 0, dataPadding)
            }
        }
        return output.toByteArray()
    }

    private fun buffer(size: Int): ByteBuffer {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
    }




    private fun Map<String, String>.replaceNodeName(node: Resources.XmlNode): Resources.XmlNode? {
        if (node.hasElement()) {
            var builder: Resources.XmlElement.Builder? = null
            node.element.childList.forEachIndexed { index, child ->
                replaceNodeName(child)?.let {
                    if (builder == null) {
                        builder = node.element.toBuilder()
                    }
                    builder?.setChild(index, it)
                }
            }

            replaceClassName(node.element.name)?.let {
                if (builder == null) {
                    builder = node.element.toBuilder()
                }
                builder?.name = it
            }

            builder?.let {
//                println("repackage.element => ${element.name} => $newName")
                return node.toBuilder().setElement(it.build()).build()
            }
//            println("repackage.element => ${element.name}")
        } else {
//            println("repackage.text => ${text}")
        }
        return null
    }

    private fun Map<String, String>.replaceClassName(className: String): String? {
        for ((k, v) in this) {
            if (className.startsWith(k)) {
                return className.replace(k, v)
            }
        }
        return null
    }
}