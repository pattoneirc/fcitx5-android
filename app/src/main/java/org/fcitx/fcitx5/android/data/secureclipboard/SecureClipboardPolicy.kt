/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.data.secureclipboard

internal object SecureClipboardPolicy {
    const val PERSISTENT_EXPIRY = Long.MAX_VALUE
    const val MAX_TEXT_LENGTH = 100_000
    const val DELETE_AFTER_PASTE = false
}
