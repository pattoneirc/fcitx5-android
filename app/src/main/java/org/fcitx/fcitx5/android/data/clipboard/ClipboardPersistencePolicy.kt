/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.clipboard

/**
 * Centralizes the privacy rule for writing clipboard content to local history.
 *
 * Sensitive clips are always memory-only. A history limit of zero (the default
 * in the personal build) also disables persistence completely.
 */
internal object ClipboardPersistencePolicy {
    fun shouldPersist(sensitive: Boolean, historyLimit: Int): Boolean =
        !sensitive && historyLimit > 0
}
