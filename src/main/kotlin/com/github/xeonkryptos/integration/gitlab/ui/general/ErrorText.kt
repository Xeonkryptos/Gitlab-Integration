package com.github.xeonkryptos.integration.gitlab.ui.general

import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Insets
import javax.swing.SwingConstants
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import org.jetbrains.annotations.NotNull

/**
 * Extracted out of [DialogWrapper] as there are cases we need an error text triggered by [ComponentValidator], but aren't in a DialogWrapper that is provided it implicitly
 */
class ErrorText : JBLabel() {

    private val errors: MutableList<ValidationInfo> = mutableListOf()

    init {
        border = createErrorTextBorder()
        horizontalAlignment = SwingConstants.LEADING
        isAllowAutoWrapping = true
        isVisible = false
    }

    private fun createErrorTextBorder(): Border {
        val border: Border = JBEmptyBorder(UIUtil.getRegularPanelInsets())
        val contentInsets: Insets = border.getBorderInsets(null)
        val baseInsets: Insets = JBInsets.create(16, 13)
        return EmptyBorder(baseInsets.top,
                           if (baseInsets.left > contentInsets.left) baseInsets.left - contentInsets.left else 0,
                           if (baseInsets.bottom > contentInsets.bottom) baseInsets.bottom - contentInsets.bottom else 0,
                           if (baseInsets.right > contentInsets.right) baseInsets.right - contentInsets.right else 0)
    }

    fun appendError(@NotNull info: ValidationInfo) {
        errors.add(info)
        val sb = StringBuilder("<html>")
        errors.forEach { vi ->
            val color: Color = if (vi.warning) MessageType.WARNING.titleForeground else UIUtil.getErrorForeground()
            sb.append("<font color='#").append(ColorUtil.toHex(color)).append("'>").append("<left>").append(vi.message).append("</left></font><br/>")
        }
        sb.append("</html>")
        text = sb.toString()
        isVisible = true
    }

    fun clearError(full: Boolean) {
        errors.clear()
        if (full) {
            setBounds(0, 0, 0, 0)
            text = ""
            isVisible = false
        }
    }

    fun isTextSet(@NotNull info: List<ValidationInfo>): Boolean = errors == info
}