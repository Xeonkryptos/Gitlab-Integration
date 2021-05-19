package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.CloneRepositoryUI.ExtraActionButton
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable
import com.intellij.ui.CommonActionsPanel
import com.intellij.ui.ListUtil
import com.intellij.ui.RowsDnDSupport
import com.intellij.ui.ToolbarDecorator
import com.intellij.util.SmartList
import com.intellij.util.ui.EditableModel
import java.beans.PropertyChangeEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListModel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

/**
 * Based on [com.intellij.ui.ListToolbarDecorator], copied it really, and added some custom code. A copy of it is required, because the base class isn't accessible (can't extend from it), needs
 * the basic functionality, but with some minor changes.<br></br>
 * Thus, this implementation handles the extra actions differently. When implementing the interface [CloneRepositoryUI.ExtraActionButton] they should be handled differently. The basic
 * implementation enables the buttons when at least one element in your list/table is selected. The goal here is to trigger a download call and replace the internal content with the received data
 * and not manipulating the internal model (positions, etc). Therefore, the enabled state is typically verified with the selection state. This behaviour is avoided, if custom extra actions are used,
 * that are implementing the interface mentioned above. Every other button behaves as the default ones -> only work with the internal model.
 */
internal class CustomListToolbarDecorator<T>(private val myList: JList<T>, private val myEditableModel: EditableModel?) : ToolbarDecorator() {

    private val myExtraActions: MutableList<AnActionButton> = SmartList()

    init {
        myDownActionEnabled = true
        myUpActionEnabled = myDownActionEnabled
        myRemoveActionEnabled = myUpActionEnabled
        myAddActionEnabled = myRemoveActionEnabled
        createActions()
        myList.addListSelectionListener { updateButtons() }
        val modelListener: ListDataListener = object : ListDataListener {
            override fun intervalAdded(e: ListDataEvent) {
                updateButtons()
            }

            override fun intervalRemoved(e: ListDataEvent) {
                updateButtons()
            }

            override fun contentsChanged(e: ListDataEvent) {
                updateButtons()
            }
        }
        myList.model.addListDataListener(modelListener)
        myList.addPropertyChangeListener("model") { evt: PropertyChangeEvent ->
            if (evt.oldValue != null) {
                (evt.oldValue as ListModel<*>).removeListDataListener(modelListener)
            }
            if (evt.newValue != null) {
                (evt.newValue as ListModel<*>).addListDataListener(modelListener)
            }
        }
        myList.addPropertyChangeListener("enabled") { updateButtons() }
    }

    private fun createActions() {
        myRemoveAction = AnActionButtonRunnable {
            ListUtil.removeSelectedItems(myList)
            updateButtons()
        }
        myUpAction = AnActionButtonRunnable {
            ListUtil.moveSelectedItemsUp(myList)
            updateButtons()
        }
        myDownAction = AnActionButtonRunnable {
            ListUtil.moveSelectedItemsDown(myList)
            updateButtons()
        }
    }

    override fun getComponent(): JComponent = myList

    override fun updateButtons() {
        val p = actionsPanel
        if (p != null) {
            val someElementSelected: Boolean
            if (myList.isEnabled) {
                val index = myList.selectedIndex
                someElementSelected = 0 <= index && index < myList.model.size
                if (someElementSelected) {
                    val downEnable = myList.maxSelectionIndex < myList.model.size - 1
                    val upEnable = myList.minSelectionIndex > 0
                    val editEnabled = myList.selectedIndices.size == 1
                    p.setEnabled(CommonActionsPanel.Buttons.EDIT, editEnabled)
                    p.setEnabled(CommonActionsPanel.Buttons.UP, upEnable)
                    p.setEnabled(CommonActionsPanel.Buttons.DOWN, downEnable)
                } else {
                    p.setEnabled(CommonActionsPanel.Buttons.EDIT, false)
                    p.setEnabled(CommonActionsPanel.Buttons.UP, false)
                    p.setEnabled(CommonActionsPanel.Buttons.DOWN, false)
                }
                p.setEnabled(CommonActionsPanel.Buttons.ADD, true)
            } else {
                someElementSelected = false
                p.setEnabled(CommonActionsPanel.Buttons.ADD, false)
                p.setEnabled(CommonActionsPanel.Buttons.UP, false)
                p.setEnabled(CommonActionsPanel.Buttons.DOWN, false)
            }
            p.setEnabled(CommonActionsPanel.Buttons.REMOVE, someElementSelected)
            updateExtraElementActions(someElementSelected)
        }
    }

    override fun addExtraAction(action: AnActionButton): ToolbarDecorator {
        myExtraActions.add(action)
        return super.addExtraAction(action)
    }

    override fun updateExtraElementActions(someElementSelected: Boolean) {
        for (myExtraAction in myExtraActions) {
            if (myExtraAction is ExtraActionButton) {
                (myExtraAction as ExtraActionButton).verifyUpdateState()
            } else {
                myExtraAction.isEnabled = someElementSelected
            }
        }
    }

    override fun setVisibleRowCount(rowCount: Int): ToolbarDecorator {
        myList.visibleRowCount = rowCount
        return this
    }

    override fun isModelEditable(): Boolean = myEditableModel != null || myList.model is EditableModel

    override fun installDnDSupport() = RowsDnDSupport.install(myList, (myEditableModel ?: myList.model as EditableModel))
}