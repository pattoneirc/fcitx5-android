/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Only encrypted payloads are stored in this table. Metadata is authenticated
 * together with the ciphertext and deliberately contains no text preview.
 */
@Entity(tableName = SecureClipboardEntry.TABLE_NAME)
data class SecureClipboardEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val ciphertext: ByteArray,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val iv: ByteArray,
    val createdAt: Long,
    val expiresAt: Long,
    val deleteAfterPaste: Boolean
) {
    companion object {
        const val TABLE_NAME = "secure_clipboard"
    }
}
