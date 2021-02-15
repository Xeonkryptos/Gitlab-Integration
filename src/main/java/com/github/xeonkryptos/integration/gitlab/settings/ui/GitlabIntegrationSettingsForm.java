package com.github.xeonkryptos.integration.gitlab.settings.ui;

import com.github.xeonkryptos.integration.gitlab.service.GitlabIntegrationSettingsService;
import com.github.xeonkryptos.integration.gitlab.service.settings.GitlabIntegrationSettings;
import com.intellij.openapi.project.Project;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * @author Xeonkryptos
 * @since 15.02.2021
 */
public class GitlabIntegrationSettingsForm {

    private final GitlabIntegrationSettings settings;

    private JPanel settingsPanel;
    private JCheckBox disableSSLCertificateValidationCheckBox;

    public GitlabIntegrationSettingsForm(Project project) {
        settings = GitlabIntegrationSettingsService.getInstance(project).getState();

        updateUiWith(settings);
    }

    public boolean isModified() {
        return disableSSLCertificateValidationCheckBox.isSelected() != settings.getDisableSslVerification();
    }

    public void apply() {
        syncSettingsWithUi();
    }

    public void reset() {
        updateUiWith(settings);
    }

    private void syncSettingsWithUi() {
        boolean disableSslCertificateValidation = disableSSLCertificateValidationCheckBox.isSelected();
        settings.setDisableSslVerification(disableSslCertificateValidation);
    }

    private void updateUiWith(GitlabIntegrationSettings gitlabIntegrationSettings) {
        boolean sslVerificationDisabled = gitlabIntegrationSettings.getDisableSslVerification();
        disableSSLCertificateValidationCheckBox.setSelected(sslVerificationDisabled);
    }

    public JPanel getSettingsPanel() {
        return settingsPanel;
    }
}
