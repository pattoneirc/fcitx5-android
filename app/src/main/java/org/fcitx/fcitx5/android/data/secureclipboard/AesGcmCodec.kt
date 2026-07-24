/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object AesGcmCodec {
    data class EncryptedPayload(
        val ciphertext: ByteArray,
        val iv: ByteArray
    )

    fun encrypt(key: SecretKey, plaintext: ByteArray, aad: ByteArray): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // Android Keystore generates the IV so callers cannot accidentally reuse one.
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad)
        return EncryptedPayload(
            ciphertext = cipher.doFinal(plaintext),
            iv = cipher.iv.copyOf()
        )
    }

    fun decrypt(
        key: SecretKey,
        ciphertext: ByteArray,
        iv: ByteArray,
        aad: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
}
