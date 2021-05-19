package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository;

import com.intellij.ui.*;
import com.intellij.util.SmartList;
import com.intellij.util.ui.EditableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.List;

/**
 * Based on {@link com.intellij.ui.ListToolbarDecorator}, copied it really, and added some custom code. A copy of it is required, because the base class isn't accessible (can't extend from it), needs
 * the basic functionality, but with some minor changes.<br/>
 * Thus, this implementation handles the extra actions differently. When implementing the interface {@linkplain CloneRepositoryUI.ExtraActionButton} they should be handled differently. The basic
 * implementation enables the buttons when at least one element in your list/table is selected. The goal here is to trigger a download call and replace the internal content with the received data
 * and not manipulating the internal model (positions, etc). Therefore, the enabled state is typically verified with the selection state. This behaviour is avoided, if custom extra actions are used,
 * that are implementing the interface mentioned above. Every other button behaves as the default ones -> only work with the internal model.
 */
class CustomListToolbarDecorator<T> extends ToolbarDecorator {
    private final JList<T> myList;
    private final EditableModel myEditableModel;
    private final List<AnActionButton> myExtraActions = new SmartList<>();

    @SuppressWarnings("unchecked")
    CustomListToolbarDecorator(@NotNull JList<T> list, @Nullable EditableModel editableModel) {
        myList = list;
        myEditableModel = editableModel;
        myAddActionEnabled = myRemoveActionEnabled = myUpActionEnabled = myDownActionEnabled = true;
        createActions();
        myList.addListSelectionListener(__ -> updateButtons());
        ListDataListener modelListener = new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                updateButtons();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                updateButtons();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                updateButtons();
            }
        };
        myList.getModel().addListDataListener(modelListener);
        myList.addPropertyChangeListener("model", evt -> {
            if (evt.getOldValue() != null) {
                ((ListModel<T>) evt.getOldValue()).removeListDataListener(modelListener);
            }
            if (evt.getNewValue() != null) {
                ((ListModel<T>) evt.getNewValue()).addListDataListener(modelListener);
            }
        });
        myList.addPropertyChangeListener("enabled", __ -> updateButtons());
    }

    private void createActions() {
        myRemoveAction = __ -> {
            ListUtil.removeSelectedItems(myList);
            updateButtons();
        };
        myUpAction = __ -> {
            ListUtil.moveSelectedItemsUp(myList);
            updateButtons();
        };
        myDownAction = __ -> {
            ListUtil.moveSelectedItemsDown(myList);
            updateButtons();
        };
    }

    @Override
    protected @NotNull JComponent getComponent() {
        return myList;
    }

    @Override
    protected void updateButtons() {
        final CommonActionsPanel p = getActionsPanel();
        if (p != null) {
            boolean someElementSelected;
            if (myList.isEnabled()) {
                final int index = myList.getSelectedIndex();
                someElementSelected = 0 <= index && index < myList.getModel().getSize();
                if (someElementSelected) {
                    final boolean downEnable = myList.getMaxSelectionIndex() < myList.getModel().getSize() - 1;
                    final boolean upEnable = myList.getMinSelectionIndex() > 0;
                    final boolean editEnabled = myList.getSelectedIndices().length == 1;
                    p.setEnabled(CommonActionsPanel.Buttons.EDIT, editEnabled);
                    p.setEnabled(CommonActionsPanel.Buttons.UP, upEnable);
                    p.setEnabled(CommonActionsPanel.Buttons.DOWN, downEnable);
                } else {
                    p.setEnabled(CommonActionsPanel.Buttons.EDIT, false);
                    p.setEnabled(CommonActionsPanel.Buttons.UP, false);
                    p.setEnabled(CommonActionsPanel.Buttons.DOWN, false);
                }
                p.setEnabled(CommonActionsPanel.Buttons.ADD, true);
            } else {
                someElementSelected = false;
                p.setEnabled(CommonActionsPanel.Buttons.ADD, false);
                p.setEnabled(CommonActionsPanel.Buttons.UP, false);
                p.setEnabled(CommonActionsPanel.Buttons.DOWN, false);
            }

            p.setEnabled(CommonActionsPanel.Buttons.REMOVE, someElementSelected);
            updateExtraElementActions(someElementSelected);
        }
    }

    @Override
    public @NotNull ToolbarDecorator addExtraAction(@NotNull AnActionButton action) {
        myExtraActions.add(action);
        return super.addExtraAction(action);
    }

    @Override
    protected void updateExtraElementActions(boolean someElementSelected) {
        for (AnActionButton myExtraAction : myExtraActions) {
            if (myExtraAction instanceof CloneRepositoryUI.ExtraActionButton) {
                ((CloneRepositoryUI.ExtraActionButton) myExtraAction).verifyUpdateState();
            } else {
                myExtraAction.setEnabled(someElementSelected);
            }
        }
    }

    @Override
    public @NotNull ToolbarDecorator setVisibleRowCount(int rowCount) {
        myList.setVisibleRowCount(rowCount);
        return this;
    }

    @Override
    protected boolean isModelEditable() {
        return myEditableModel != null || myList.getModel() instanceof EditableModel;
    }

    @Override
    protected void installDnDSupport() {
        RowsDnDSupport.install(myList, myEditableModel != null ? myEditableModel : (EditableModel) myList.getModel());
    }
}
