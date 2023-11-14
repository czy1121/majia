package me.reezy.gradle.majia.plugin

import java.io.File
import java.security.MessageDigest

fun File.touch(): File {
    if (!this.exists()) {
        this.parentFile?.mkdirs()
        this.createNewFile()
    }
    return this
}


fun ByteArray.hex(): String {
    val hex = StringBuilder(size * 2)
    for (b in this) {
        if (b.toInt() and 0xff < 0x10) {
            hex.append("0")
        }
        hex.append(Integer.toHexString(b.toInt() and 0xff))
    }
    return hex.toString()
}

fun ByteArray.sha256(): ByteArray = digest("SHA-256")

fun ByteArray.digest(algorithm: String): ByteArray = MessageDigest.getInstance(algorithm).digest(this)



