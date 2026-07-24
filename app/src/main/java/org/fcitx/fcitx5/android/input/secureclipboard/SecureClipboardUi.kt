/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Vincent
 */
package org.fcitx.fcitx5.android.input.secureclipboard

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ViewAnimator
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.ui.ToolButton
import org.fcitx.fcitx5.android.input.clipboard.SpacesItemDecoration
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.coordinatorlayout.coordinatorLayout
import splitties.views.dsl.coordinatorlayout.defaultLParams
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.view
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.setPaddingDp

class SecureClipboardUi(override val ctx: Context, private val theme: Theme) : Ui {
    val recyclerView = recyclerView {
        addItemDecoration(SpacesItemDecoration(dp(4)))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        filterTouchesWhenObscured = true
    }

    private val emptyView = textView {
        setText(R.string.secure_clipboard_empty)
        setTextColor(theme.keyTextColor)
        textSize = 15f
        gravity = Gravity.CENTER
        setPaddingDp(24)
    }

    private val animator = view(::ViewAnimator) {
        add(recyclerView, lParams(matchParent, matchParent))
        add(emptyView, lParams(matchParent, matchParent))
    }

    private val keyBorder by ThemeManager.prefs.keyBorder

    override val root = coordinatorLayout {
        if (!keyBorder) backgroundColor = theme.barColor
        add(animator, defaultLParams(matchParent, matchParent))
    }

    val normalClipboardButton = ToolButton(ctx, R.drawable.ic_clipboard, theme).apply {
        contentDescription = ctx.getString(R.string.clipboard)
    }

    val deleteAllButton = ToolButton(ctx, R.drawable.ic_baseline_delete_sweep_24, theme).apply {
        contentDescription = ctx.getString(R.string.delete_all)
    }

    val importClipboardButton = ToolButton(ctx, R.drawable.ic_baseline_save_24, theme).apply {
        contentDescription = ctx.getString(R.string.secure_import)
    }

    val extension = horizontalLayout {
        add(normalClipboardButton, lParams(dp(40), dp(40)))
        add(importClipboardButton, lParams(dp(40), dp(40)))
        add(deleteAllButton, lParams(dp(40), dp(40)))
    }

    fun setEmpty(empty: Boolean) {
        animator.displayedChild = if (empty) 1 else 0
    }
}
