package com.gapps.utils

import org.apache.commons.codec.binary.Base64
import kotlin.experimental.xor


class Cipher(private val password: String) {

    fun encrypt(text: String): String {
        return String(Base64.encodeBase64(this.xor(text.toByteArray())))
    }

    fun decrypt(hash: String): String {
        try {
            return String(this.xor(Base64.decodeBase64(hash.toByteArray())))
        } catch (ex: java.io.UnsupportedEncodingException) {
            throw IllegalStateException(ex)
        }
    }

    private fun xor(input: ByteArray): ByteArray {
        val output = ByteArray(input.size)
        val secret = password.toByteArray()
        var spos = 0
        for (pos in input.indices) {
            output[pos] = (input[pos] xor secret[spos])
            spos += 1
            if (spos >= secret.size) {
                spos = 0
            }
        }
        return output
    }
}