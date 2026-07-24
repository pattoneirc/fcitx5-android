/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SecureClipboardEntry::class],
    version = 1
)
abstract class SecureClipboardDatabase : RoomDatabase() {
    abstract fun secureClipboardDao(): SecureClipboardDao
}
