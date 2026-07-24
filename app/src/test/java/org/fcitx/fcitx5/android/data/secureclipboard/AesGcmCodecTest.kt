/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator

class AesGcmCodecTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun roundTripPreservesPlaintextWithoutStoringIt() {
        val plaintext = "机密内容 GitHub token".toByteArray()
        val aad = "metadata".toByteArray()
        val encrypted = AesGcmCodec.encrypt(key, plaintext, aad)

        assertFalse(encrypted.ciphertext.contentEquals(plaintext))
        assertArrayEquals(
            plaintext,
            AesGcmCodec.decrypt(key, encrypted.ciphertext, encrypted.iv, aad)
        )
    }

    @Test(expected = AEADBadTagException::class)
    fun modifiedCiphertextCannotBeDecrypted() {
        val encrypted = AesGcmCodec.encrypt(key, "secret".toByteArray(), "metadata".toByteArray())
        encrypted.ciphertext[0] = (encrypted.ciphertext[0].toInt() xor 1).toByte()

        AesGcmCodec.decrypt(
            key,
            encrypted.ciphertext,
            encrypted.iv,
            "metadata".toByteArray()
        )
    }

    @Test(expected = AEADBadTagException::class)
    fun modifiedMetadataCannotBeDecrypted() {
        val encrypted = AesGcmCodec.encrypt(key, "secret".toByteArray(), "metadata".toByteArray())

        AesGcmCodec.decrypt(
            key,
            encrypted.ciphertext,
            encrypted.iv,
            "changed-metadata".toByteArray()
        )
    }
}
