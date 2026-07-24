/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.clipboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardPersistencePolicyTest {

    @Test
    fun sensitiveClipIsNeverPersisted() {
        assertFalse(ClipboardPersistencePolicy.shouldPersist(sensitive = true, historyLimit = 10))
    }

    @Test
    fun zeroHistoryKeepsRegularClipInMemoryOnly() {
        assertFalse(ClipboardPersistencePolicy.shouldPersist(sensitive = false, historyLimit = 0))
    }

    @Test
    fun negativeHistoryLimitDoesNotPersist() {
        assertFalse(ClipboardPersistencePolicy.shouldPersist(sensitive = false, historyLimit = -1))
    }

    @Test
    fun optedInHistoryPersistsRegularClip() {
        assertTrue(ClipboardPersistencePolicy.shouldPersist(sensitive = false, historyLimit = 10))
    }
}
