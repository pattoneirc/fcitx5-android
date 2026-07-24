/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureClipboardDao {
    @Insert
    suspend fun insert(entry: SecureClipboardEntry): Long

    @Query(
        "SELECT * FROM ${SecureClipboardEntry.TABLE_NAME} " +
            "ORDER BY createdAt DESC"
    )
    fun allEntries(): Flow<List<SecureClipboardEntry>>

    @Query(
        "SELECT * FROM ${SecureClipboardEntry.TABLE_NAME} " +
            "WHERE id=:id LIMIT 1"
    )
    suspend fun get(id: Int): SecureClipboardEntry?

    @Query("DELETE FROM ${SecureClipboardEntry.TABLE_NAME} WHERE id=:id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM ${SecureClipboardEntry.TABLE_NAME} WHERE expiresAt<=:now")
    suspend fun deleteExpired(now: Long)

    @Query("SELECT MIN(expiresAt) FROM ${SecureClipboardEntry.TABLE_NAME}")
    suspend fun nextExpiry(): Long?

    @Query("DELETE FROM ${SecureClipboardEntry.TABLE_NAME}")
    suspend fun deleteAll()
}
