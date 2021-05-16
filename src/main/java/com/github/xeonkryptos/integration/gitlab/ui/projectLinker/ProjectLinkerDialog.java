package com.github.xeonkryptos.integration.gitlab.ui.projectLinker;

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle;
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBComboBoxLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.List;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectLinkerDialog extends DialogWrapper {

    private JPanel contentPane;

    private JBTextField projectPathTxtField;
    private JBTextField projectNameTxtField;
    private JBTextField remoteTxtField;
    private JBTextField descriptionTxtField;
    private ComboBox<GitlabAccount> accountComboBox;
    private JBCheckBox privateProjectCheckBox;
    private ActionLink chooseGroupActionLink;
    private ActionLink newAccountActionLink;

    public ProjectLinkerDialog(Project project) {
        super(project, true, IdeModalityType.PROJECT);
        init();
        setTitle("Share Project");
        centerRelativeToParent();
        setOKButtonText(GitlabBundle.message("share.button"));
        setHorizontalStretch(1.3f);

        accountComboBox.addItemListener(itemEvent -> projectPathTxtField.setText(null));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    public void fillWithDefaultValues(String moduleName, List<GitlabAccount> gitlabAccounts) {
        projectNameTxtField.setText(moduleName);
        CollectionComboBoxModel<GitlabAccount> gitlabAccountComboBoxModel = new CollectionComboBoxModel<>(gitlabAccounts);
        accountComboBox.setModel(gitlabAccountComboBoxModel);
        if (!gitlabAccounts.isEmpty()) {
            accountComboBox.setSelectedIndex(0);
        } else {
            updateEnableStateOfOkButton();
        }
    }

    private void updateEnableStateOfOkButton() {
        boolean enable = accountComboBox.getItem() != null;
    }

    private void createUIComponents() {
        contentPane = new DialogPanel();
    }
}
