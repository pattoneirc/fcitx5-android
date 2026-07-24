/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.input.secureclipboard

import android.text.format.DateFormat
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.secureclipboard.db.SecureClipboardEntry
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.clipboard.ClipboardEntryUi
import org.fcitx.fcitx5.android.utils.item
import splitties.resources.styledColor
import java.util.Date

abstract class SecureClipboardAdapter(
    private val theme: Theme,
    private val entryRadius: Float
) : ListAdapter<SecureClipboardEntry, SecureClipboardAdapter.ViewHolder>(diffCallback) {

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<SecureClipboardEntry>() {
            override fun areItemsTheSame(
                oldItem: SecureClipboardEntry,
                newItem: SecureClipboardEntry
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: SecureClipboardEntry,
                newItem: SecureClipboardEntry
            ): Boolean =
                oldItem.createdAt == newItem.createdAt &&
                    oldItem.expiresAt == newItem.expiresAt &&
                    oldItem.deleteAfterPaste == newItem.deleteAfterPaste
        }
    }

    class ViewHolder(val entryUi: ClipboardEntryUi) : RecyclerView.ViewHolder(entryUi.root)

    private var popupMenu: PopupMenu? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ClipboardEntryUi(parent.context, theme, entryRadius))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        with(holder.entryUi) {
            val expiry = DateFormat.getTimeFormat(ctx).format(Date(entry.expiresAt))
            setEntry(
                ctx.getString(
                    R.string.secure_clipboard_entry,
                    expiry
                ),
                false
            )
            root.setOnClickListener { onPaste(entry) }
            root.setOnLongClickListener {
                popupMenu?.dismiss()
                popupMenu = PopupMenu(ctx, root).apply {
                    menu.item(
                        R.string.delete,
                        R.drawable.ic_baseline_delete_24,
                        ctx.styledColor(android.R.attr.colorControlNormal)
                    ) {
                        onDelete(entry.id)
                    }
                    setOnDismissListener {
                        if (it === popupMenu) popupMenu = null
                    }
                    show()
                }
                true
            }
        }
    }

    fun onDetached() {
        popupMenu?.dismiss()
        popupMenu = null
    }

    abstract fun onPaste(entry: SecureClipboardEntry)

    abstract fun onDelete(id: Int)
}
