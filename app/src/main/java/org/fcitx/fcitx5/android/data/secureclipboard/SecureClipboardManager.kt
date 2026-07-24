/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.data.secureclipboard.db.SecureClipboardDao
import org.fcitx.fcitx5.android.data.secureclipboard.db.SecureClipboardDatabase
import org.fcitx.fcitx5.android.data.secureclipboard.db.SecureClipboardEntry
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object SecureClipboardManager {
    data class DecryptedEntry(
        val id: Int,
        val text: String,
        val deleteAfterPaste: Boolean
    )

    private lateinit var database: SecureClipboardDatabase
    private lateinit var dao: SecureClipboardDao
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var expiryCleanupJob: Job? = null

    @Volatile
    var isInitialized: Boolean = false
        private set

    fun init(context: Context) {
        if (isInitialized) return
        database = Room.databaseBuilder(
            context,
            SecureClipboardDatabase::class.java,
            DATABASE_NAME
        ).build()
        dao = database.secureClipboardDao()
        isInitialized = true
        scheduleExpiryCleanup()
    }

    fun allEntries(): Flow<List<SecureClipboardEntry>> {
        check(isInitialized) { "Secure clipboard is unavailable before device unlock" }
        return dao.allEntries()
    }

    suspend fun save(text: String): Long = withContext(Dispatchers.IO) {
        check(isInitialized) { "Secure clipboard is unavailable before device unlock" }
        require(text.isNotEmpty()) { "Cannot encrypt empty text" }
        require(text.length <= SecureClipboardPolicy.MAX_TEXT_LENGTH) {
            "Selected text is too long"
        }

        val createdAt = System.currentTimeMillis()
        val expiresAt = SecureClipboardPolicy.expiresAt(createdAt)
        val deleteAfterPaste = SecureClipboardPolicy.DELETE_AFTER_PASTE
        val plaintext = text.toByteArray(StandardCharsets.UTF_8)
        try {
            val encrypted = AesGcmCodec.encrypt(
                key = getOrCreateKey(),
                plaintext = plaintext,
                aad = metadataAad(
                    createdAt = createdAt,
                    expiresAt = expiresAt,
                    deleteAfterPaste = deleteAfterPaste
                ),
            )
            dao.deleteExpired(createdAt)
            val rowId = dao.insert(
                SecureClipboardEntry(
                    ciphertext = encrypted.ciphertext,
                    iv = encrypted.iv,
                    createdAt = createdAt,
                    expiresAt = expiresAt,
                    deleteAfterPaste = deleteAfterPaste
                )
            )
            scheduleExpiryCleanup()
            rowId
        } finally {
            plaintext.fill(0)
        }
    }

    suspend fun decrypt(id: Int): DecryptedEntry? = withContext(Dispatchers.IO) {
        check(isInitialized) { "Secure clipboard is unavailable before device unlock" }
        val entry = dao.get(id) ?: return@withContext null
        if (SecureClipboardPolicy.isExpired(entry.expiresAt, System.currentTimeMillis())) {
            dao.delete(entry.id)
            return@withContext null
        }

        try {
            val plaintext = AesGcmCodec.decrypt(
                key = getOrCreateKey(),
                ciphertext = entry.ciphertext,
                iv = entry.iv,
                aad = metadataAad(
                    createdAt = entry.createdAt,
                    expiresAt = entry.expiresAt,
                    deleteAfterPaste = entry.deleteAfterPaste
                ),
            )
            try {
                DecryptedEntry(
                    id = entry.id,
                    text = plaintext.toString(StandardCharsets.UTF_8),
                    deleteAfterPaste = entry.deleteAfterPaste
                )
            } finally {
                plaintext.fill(0)
            }
        } catch (exception: Exception) {
            Timber.w(exception, "Unable to decrypt secure clipboard entry %d", entry.id)
            null
        }
    }

    suspend fun deleteAfterSuccessfulPaste(entry: DecryptedEntry) {
        if (entry.deleteAfterPaste) delete(entry.id)
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        if (isInitialized) dao.delete(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        if (isInitialized) dao.deleteAll()
    }

    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        if (isInitialized) dao.deleteExpired(System.currentTimeMillis())
    }

    @Synchronized
    private fun scheduleExpiryCleanup() {
        expiryCleanupJob?.cancel()
        expiryCleanupJob = cleanupScope.launch {
            while (isActive) {
                val nextExpiry = dao.nextExpiry() ?: break
                delay((nextExpiry - System.currentTimeMillis()).coerceAtLeast(0L))
                dao.deleteExpired(System.currentTimeMillis())
            }
        }
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        ).apply {
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setUnlockedDeviceRequired(true)
                    }
                }
                .build()
            init(spec)
        }.generateKey()
    }

    private fun metadataAad(
        createdAt: Long,
        expiresAt: Long,
        deleteAfterPaste: Boolean
    ): ByteArray = "$AAD_VERSION|$createdAt|$expiresAt|$deleteAfterPaste"
        .toByteArray(StandardCharsets.UTF_8)

    private const val DATABASE_NAME = "secure_clbdb"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "vincent_secure_clipboard_aes_v1"
    private const val AAD_VERSION = "secure-clipboard-v1"
}
