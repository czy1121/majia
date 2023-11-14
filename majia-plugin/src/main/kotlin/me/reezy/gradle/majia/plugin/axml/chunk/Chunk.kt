package me.reezy.gradle.majia.plugin.axml.chunk

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class Chunk(val chunkType: Short, buffer: ByteBuffer) {
    val headerSize: Short = buffer.short
    var chunkSize: Int = buffer.int


    protected abstract fun writeBodyTo(stream: OutputStream)

    fun writeTo(stream: OutputStream) {
        val body = ByteArrayOutputStream().apply { writeBodyTo(this) }.toByteArray()

        chunkSize = 8 + body.size

        stream.write(buffer(8).putShort(chunkType).putShort(headerSize).putInt(chunkSize).array())
        stream.write(body)
    }

    companion object {
        const val TYPE_STRING_POOL: Short = 0x0001
        const val TYPE_RESOURCE_MAP: Short = 0x0180
        const val TYPE_START_NAMESPACE: Short = 0x0100
        const val TYPE_END_NAMESPACE: Short = 0x0101
        const val TYPE_START_ELEMENT: Short = 0x0102
        const val TYPE_END_ELEMENT: Short = 0x0103
        const val TYPE_CDATA: Short = 0x0104


        internal fun buffer(size: Int): ByteBuffer {
            return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        }
    }
}
