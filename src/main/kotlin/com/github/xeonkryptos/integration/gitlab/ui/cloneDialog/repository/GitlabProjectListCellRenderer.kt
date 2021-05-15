package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.ui.CellRendererPanel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JList

class GitlabProjectListCellRenderer(private val accountsSupplier: () -> Collection<GitlabAccount>) : ColoredListCellRenderer<GitlabProjectListItem>() {

    private val nameRenderer = AccountNameRenderer()

    override fun getListCellRendererComponent(
        list: JList<out GitlabProjectListItem>, value: GitlabProjectListItem, index: Int, selected: Boolean, hasFocus: Boolean
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, selected, hasFocus)
        if (showAccountNameAbove(list, index)) {
            val name = value.gitlabAccount.username
            return nameRenderer.withName(name, component, index != 0)
        }
        return component
    }

    private fun showAccountNameAbove(list: JList<out GitlabProjectListItem>, index: Int): Boolean =
        accountsSupplier().size > 1 && (index == 0 || list.model.getElementAt(index).gitlabAccount != list.model.getElementAt(index - 1).gitlabAccount)

    override fun customizeCellRenderer(
        list: JList<out GitlabProjectListItem>, value: GitlabProjectListItem, index: Int, selected: Boolean, hasFocus: Boolean
    ) {
        value.customizeRenderer(this)
    }

    private class AccountNameRenderer : CellRendererPanel() {
        private val titleLabel = SimpleColoredComponent().apply {
            background = UIUtil.getListBackground()
        }
        private val topLine = JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.listSeparatorColor(), 1, 0, 0, 0)
        private val borderLayout = BorderLayout()

        init {
            layout = borderLayout
            add(titleLabel, BorderLayout.NORTH)
            background = UIUtil.getListBackground()
        }

        fun withName(title: String, itemContent: Component, withBorder: Boolean): AccountNameRenderer {
            titleLabel.border = null
            titleLabel.clear()
            titleLabel.append(title, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
            if (withBorder) {
                titleLabel.border = topLine
            }
            val prevContent = borderLayout.getLayoutComponent(BorderLayout.CENTER)
            if (prevContent != null) {
                remove(prevContent)
            }
            add(itemContent, BorderLayout.CENTER)
            return this
        }
    }
}

