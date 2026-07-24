/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SecureClipboardPolicyTest {
    @Test
    fun secureEntriesArePersistentByDefault() {
        assertEquals(Long.MAX_VALUE, SecureClipboardPolicy.PERSISTENT_EXPIRY)
    }

    @Test
    fun secureEntriesRemainAfterPasteByDefault() {
        assertFalse(SecureClipboardPolicy.DELETE_AFTER_PASTE)
    }
}
