package me.reezy.gradle.majia.plugin.axml.chunk

import java.io.OutputStream
import java.nio.ByteBuffer


class StringPool(buffer: ByteBuffer) : Chunk(TYPE_STRING_POOL, buffer) {

    private val chunkStart: Int = buffer.position() - 8

    var stringCount: Int = buffer.int
    var styleCount: Int = buffer.int
    var flags: Int = buffer.int
    var stringStart: Int = buffer.int
    var styleStart: Int = buffer.int

    var stringOffsets: IntArray = IntArray(stringCount)
    var styleOffsets: IntArray = IntArray(styleCount)
    var stringList: MutableList<String> = ArrayList(stringCount)

    val isUtf8: Boolean get() = flags and FLAG_UTF8 == FLAG_UTF8
    val isSorted: Boolean get() = flags and FLAG_SORTED == FLAG_SORTED

    init {

        for (i in stringOffsets.indices) {
            stringOffsets[i] = buffer.int
        }

        for (i in styleOffsets.indices) {
            styleOffsets[i] = buffer.int
        }

        for (i in 0 until stringCount) {
            buffer.position(chunkStart + stringStart + stringOffsets[i])
            val byteCount: Int = if (isUtf8) {
                val b1 = buffer.get().toInt() and 0xFF
                val b2 = buffer.get().toInt() and 0xFF
                if (b1 > 127) {
                    val b3 = buffer.get().toInt() and 0xFF
                    val b4 = buffer.get().toInt() and 0xFF
                    b3 shl 8 or b4 and 0x7FFF
                } else if (b2 > 127) {
                    val b3 = buffer.get().toInt() and 0xFF
                    b2 shl 8 or b3 and 0x7FFF
                } else {
                    b2 and 0xFF
                }
            } else {
                buffer.short * 2
            }
            val string = ByteArray(byteCount)
            buffer.get(string)
            stringList.add(String(string, if (isUtf8) Charsets.UTF_8 else Charsets.UTF_16LE))
        }

        buffer.position(chunkStart + chunkSize)
    }

    override fun writeBodyTo(stream: OutputStream) {
        stringCount = stringList.size

        val buffer = buffer(5 * 4 + stringOffsets.size * 4 + styleOffsets.size * 4)
        buffer.putInt(stringCount)
        buffer.putInt(styleCount)
        buffer.putInt(flags)
        buffer.putInt(stringStart)
        buffer.putInt(styleStart)

        if (stringOffsets.size != stringCount) {
            stringOffsets = IntArray(stringCount)
        }
        var stringOffset = 0
        val strings = Array(stringCount) { byteArrayOf() }
        for (i in 0 until stringCount) {
            stringOffsets[i] = stringOffset
            buffer.putInt(stringOffset)

            val bytes = getStringBytes(stringList[i])
            stringOffset += bytes.size
            strings[i] = bytes
        }

        for (offset in styleOffsets) {
            buffer.putInt(offset)
        }

        stream.write(buffer.array())

        for (bytes in strings) {
            stream.write(bytes)
        }

        val pad = ((4 - stringOffset % 4) % 4)
        if (pad > 0) {
            stream.write(ByteArray(pad))
        }
    }

    private fun getStringBytes(string: String): ByteArray {
        if (isUtf8) {
            val bytes = string.toByteArray(Charsets.UTF_8)
            val lenSize = when {
                string.length > 127 -> 4
                bytes.size > 127 -> 3
                else -> 2
            }
            val buffer = buffer(lenSize + bytes.size + 1)
            if (string.length > 127) {
                buffer.put((string.length shr 8 or 0x80).toByte())
                buffer.put((string.length and 0xFF).toByte())
            } else {
                buffer.put(string.length.toByte())
            }
            if (bytes.size > 127) {
                buffer.put((bytes.size shr 8 or 0x80).toByte())
                buffer.put((bytes.size and 0xFF).toByte())
            } else {
                buffer.put(bytes.size.toByte())
            }
            buffer.put(bytes)
            buffer.put(0.toByte())
            return buffer.array()
        } else {
            val bytes = string.toByteArray(Charsets.UTF_16LE)
            val buffer = buffer(2 + bytes.size + 2)
            buffer.putShort(string.length.toShort())
            buffer.put(bytes)
            buffer.putShort(0.toShort())
            return buffer.array()
        }
    }

    companion object {
        const val FLAG_SORTED = 1
        const val FLAG_UTF8 = 1 shl 8
    }
}