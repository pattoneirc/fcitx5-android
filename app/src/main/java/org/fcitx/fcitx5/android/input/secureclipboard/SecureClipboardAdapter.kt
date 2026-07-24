/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.input.secureclipboard

import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.secureclipboard.SecureClipboardManager
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.clipboard.ClipboardEntryUi
import org.fcitx.fcitx5.android.utils.item
import splitties.resources.styledColor

abstract class SecureClipboardAdapter(
    private val theme: Theme,
    private val entryRadius: Float
) : ListAdapter<SecureClipboardManager.DecryptedEntry, SecureClipboardAdapter.ViewHolder>(
    diffCallback
) {

    companion object {
        private val diffCallback =
            object : DiffUtil.ItemCallback<SecureClipboardManager.DecryptedEntry>() {
                override fun areItemsTheSame(
                    oldItem: SecureClipboardManager.DecryptedEntry,
                    newItem: SecureClipboardManager.DecryptedEntry
                ): Boolean = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: SecureClipboardManager.DecryptedEntry,
                    newItem: SecureClipboardManager.DecryptedEntry
                ): Boolean =
                    oldItem.createdAt == newItem.createdAt && oldItem.text == newItem.text
            }
    }

    class ViewHolder(val entryUi: ClipboardEntryUi) : RecyclerView.ViewHolder(entryUi.root)

    private var popupMenu: PopupMenu? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ClipboardEntryUi(parent.context, theme, entryRadius))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        with(holder.entryUi) {
            setEntry(entry.text, false)
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

    override fun onViewRecycled(holder: ViewHolder) {
        holder.entryUi.setEntry("", false)
        holder.entryUi.root.setOnClickListener(null)
        holder.entryUi.root.setOnLongClickListener(null)
        super.onViewRecycled(holder)
    }

    fun onDetached() {
        popupMenu?.dismiss()
        popupMenu = null
        submitList(emptyList())
    }

    abstract fun onPaste(entry: SecureClipboardManager.DecryptedEntry)

    abstract fun onDelete(id: Int)
}
