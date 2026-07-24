/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.input.secureclipboard

import android.content.ClipData
import android.os.Build
import android.view.View
import android.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.secureclipboard.SecureClipboardManager
import org.fcitx.fcitx5.android.data.secureclipboard.SecureClipboardPolicy
import org.fcitx.fcitx5.android.data.secureclipboard.db.SecureClipboardEntry
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.clipboard.ClipboardWindow
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.clipboardManager
import org.fcitx.fcitx5.android.utils.toast
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp

class SecureClipboardWindow : InputWindow.ExtendedInputWindow<SecureClipboardWindow>() {
    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val windowManager: InputWindowManager by manager.must()
    private val theme by manager.theme()

    private val clipboardReturnAfterPaste by
        AppPrefs.getInstance().clipboard.clipboardReturnAfterPaste
    private val clipboardEntryRadius by ThemeManager.prefs.clipboardEntryRadius

    private var entriesJob: Job? = null
    private var promptMenu: PopupMenu? = null

    private val adapter by lazy {
        object : SecureClipboardAdapter(
            theme,
            context.dp(clipboardEntryRadius.toFloat())
        ) {
            override fun onPaste(entry: SecureClipboardEntry) {
                paste(entry.id)
            }

            override fun onDelete(id: Int) {
                service.lifecycleScope.launch {
                    SecureClipboardManager.delete(id)
                }
            }
        }
    }

    private val ui by lazy {
        SecureClipboardUi(context, theme).apply {
            recyclerView.apply {
                layoutManager =
                    StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                adapter = this@SecureClipboardWindow.adapter
            }
            normalClipboardButton.setOnClickListener {
                windowManager.attachWindow(ClipboardWindow())
            }
            importClipboardButton.setOnClickListener {
                importSystemClipboard()
            }
            deleteAllButton.setOnClickListener {
                confirmDeleteAll()
            }
        }
    }

    override fun onCreateView(): View = ui.root

    override fun onAttached() {
        if (!SecureClipboardManager.isInitialized) {
            context.toast(R.string.secure_clipboard_unavailable)
            ui.setEmpty(true)
            return
        }
        entriesJob = service.lifecycleScope.launch {
            SecureClipboardManager.purgeExpired()
            SecureClipboardManager.allEntries().collect {
                adapter.submitList(it)
                ui.setEmpty(it.isEmpty())
            }
        }
    }

    override fun onDetached() {
        entriesJob?.cancel()
        entriesJob = null
        adapter.onDetached()
        promptMenu?.dismiss()
        promptMenu = null
    }

    private fun paste(id: Int) {
        service.lifecycleScope.launch {
            if (service.currentInputConnection == null) return@launch
            val entry = SecureClipboardManager.decrypt(id)
            if (entry == null) {
                context.toast(R.string.secure_clipboard_expired)
                return@launch
            }
            service.commitText(entry.text)
            SecureClipboardManager.deleteAfterSuccessfulPaste(entry)
            if (clipboardReturnAfterPaste) {
                windowManager.attachWindow(KeyboardWindow)
            }
        }
    }

    private fun importSystemClipboard() {
        if (AppPrefs.getInstance().clipboard.clipboardListening.getValue()) {
            context.toast(R.string.secure_import_requires_history_off)
            return
        }
        val text = context.clipboardManager.primaryClip
            ?.getItemAt(0)
            ?.text
            ?.toString()
            .orEmpty()
        if (text.isEmpty()) {
            context.toast(R.string.secure_import_empty)
            return
        }
        if (text.length > SecureClipboardPolicy.MAX_TEXT_LENGTH) {
            context.toast(R.string.secure_copy_too_long)
            return
        }
        service.lifecycleScope.launch {
            runCatching {
                SecureClipboardManager.save(text)
            }.onSuccess {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.clipboardManager.clearPrimaryClip()
                } else {
                    context.clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                context.toast(R.string.secure_import_saved)
            }.onFailure {
                context.toast(R.string.secure_copy_failed)
            }
        }
    }

    private fun confirmDeleteAll() {
        promptMenu?.dismiss()
        promptMenu = PopupMenu(context, ui.deleteAllButton).apply {
            menu.add(R.string.secure_clipboard_delete_all).isEnabled = false
            menu.add(android.R.string.cancel)
            menu.item(android.R.string.ok) {
                service.lifecycleScope.launch {
                    SecureClipboardManager.deleteAll()
                }
            }
            setOnDismissListener {
                if (it === promptMenu) promptMenu = null
            }
            show()
        }
    }

    override val title: String by lazy {
        context.getString(R.string.secure_clipboard)
    }

    override fun onCreateBarExtension(): View = ui.extension
}
