/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard

internal object SecureClipboardPolicy {
    const val DEFAULT_TTL_MILLIS = 60L * 60L * 1000L
    const val MAX_TEXT_LENGTH = 100_000
    const val DELETE_AFTER_PASTE = true

    fun expiresAt(createdAt: Long): Long = createdAt + DEFAULT_TTL_MILLIS

    fun isExpired(expiresAt: Long, now: Long): Boolean = expiresAt <= now
}
