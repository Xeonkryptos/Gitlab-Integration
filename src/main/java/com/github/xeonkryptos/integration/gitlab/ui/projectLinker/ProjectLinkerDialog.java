package com.github.xeonkryptos.integration.gitlab.ui.projectLinker;

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle;
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import java.util.List;
import javax.swing.*;
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
    private ActionLink manageAccountsActionLink;

    public ProjectLinkerDialog(Project project) {
        super(project, true, IdeModalityType.PROJECT);
        ((DialogPanel) contentPane).setPreferredFocusedComponent(accountComboBox);

        init();
        setTitle(GitlabBundle.message("share.dialog.module.title"));
        centerRelativeToParent();
        setOKButtonText(GitlabBundle.message("share.button"));
        setHorizontalStretch(1.3f);

        accountComboBox.addItemListener(itemEvent -> projectPathTxtField.setText(null));

        Disposable disposable = getDisposable();
        new ComponentValidator(disposable).withValidator(() -> {
            if (accountComboBox.getSelectedItem() != null) {
                return new ValidationInfo(GitlabBundle.message("share.missing.account"), accountComboBox);
            }
            return null;
        }).andStartOnFocusLost().installOn(accountComboBox);
        new ComponentValidator(disposable).withValidator(() -> {
            String projectName = projectNameTxtField.getText();
            String trimmedProjectName = StringUtil.trim(projectName);
            if (StringUtil.isEmpty(trimmedProjectName)) {
                return new ValidationInfo(GitlabBundle.message("share.missing.project.name"), projectNameTxtField);
            }
            return null;
        }).installOn(projectNameTxtField);
        new ComponentValidator(disposable).withValidator(() -> {
            String remoteName = remoteTxtField.getText();
            String trimmedRemoteName = StringUtil.trim(remoteName);
            if (StringUtil.isEmpty(trimmedRemoteName)) {
                return new ValidationInfo(GitlabBundle.message("share.missing.remote.name"), remoteTxtField);
            }
            return null;
        }).installOn(remoteTxtField);
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
        }
    }

    private void createUIComponents() {
        contentPane = new DialogPanel();
    }
}
