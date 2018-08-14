package com.gapps.bitmexhelper.kotlin

import com.gapps.bitmexhelper.kotlin.utils.Cipher
import org.junit.Test


class CipherTest {

    @Test
    fun testEncryption() {
        val wrapper = Cipher("fooboo")
        val input = "das ist schön"
        val encrypted = wrapper.encrypt(input)
        println(encrypted)
        val decrypted = wrapper.decrypt(encrypted)
        assert(input == decrypted)
    }
}