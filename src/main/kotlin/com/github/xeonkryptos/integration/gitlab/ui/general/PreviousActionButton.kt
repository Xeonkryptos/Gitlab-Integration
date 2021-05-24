package com.github.xeonkryptos.integration.gitlab.ui.general

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.ui.AnActionButton
import com.intellij.ui.CommonActionsPanel
import com.intellij.util.IconUtil

class PreviousActionButton(title: String, private val hasPreviousRepository: () -> Boolean, private val loadPreviousPage: (source: Any) -> Unit) : AnActionButton(title, IconUtil.getMoveUpIcon()),
                                                                                                                                                   ExtraActionButton {

    init {
        addCustomUpdater { hasPreviousRepository() }
    }

    override fun getShortcut(): ShortcutSet {
        return CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.UP)
    }

    override fun actionPerformed(e: AnActionEvent) {
        loadPreviousPage(this)
    }

    override fun verifyUpdateState() {
        isEnabled = hasPreviousRepository()
    }
}