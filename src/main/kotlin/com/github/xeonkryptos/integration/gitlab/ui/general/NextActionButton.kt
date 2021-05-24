package com.github.xeonkryptos.integration.gitlab.ui.general

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.ui.AnActionButton
import com.intellij.ui.CommonActionsPanel
import com.intellij.util.IconUtil

class NextActionButton(title: String, private val hasNextRepository: () -> Boolean, private val loadNextPage: (source: Any) -> Unit) : AnActionButton(title, IconUtil.getMoveDownIcon()),
                                                                                                                                       ExtraActionButton {

    init {
        addCustomUpdater { hasNextRepository() }
    }

    override fun getShortcut(): ShortcutSet {
        return CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.DOWN)
    }

    override fun actionPerformed(e: AnActionEvent) {
        loadNextPage(this)
    }

    override fun verifyUpdateState() {
        isEnabled = hasNextRepository()
    }
}