package me.reezy.gradle.majia.plugin.axml.chunk

import java.io.OutputStream
import java.nio.ByteBuffer

class ResourceMap(buffer: ByteBuffer) : Chunk(TYPE_RESOURCE_MAP, buffer) {

    val resourceIds: MutableList<Int>

    init {
        val count = (chunkSize - 8) / 4
        resourceIds = ArrayList(count)
        for (i in 0 until count) {
            resourceIds.add(buffer.int)
        }
    }

    override fun writeBodyTo(stream: OutputStream) {
        val buffer = buffer(resourceIds.size * 4)
        for (integer in resourceIds) {
            buffer.putInt(integer)
        }
        stream.write(buffer.array())
    }
}