/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureClipboardPolicyTest {
    @Test
    fun defaultExpiryIsOneHour() {
        val createdAt = 10_000L
        assertEquals(
            createdAt + 60L * 60L * 1000L,
            SecureClipboardPolicy.expiresAt(createdAt)
        )
    }

    @Test
    fun expiryBoundaryIsInclusive() {
        assertFalse(SecureClipboardPolicy.isExpired(expiresAt = 20L, now = 19L))
        assertTrue(SecureClipboardPolicy.isExpired(expiresAt = 20L, now = 20L))
    }

    @Test
    fun secureEntriesBurnAfterPasteByDefault() {
        assertTrue(SecureClipboardPolicy.DELETE_AFTER_PASTE)
    }
}
