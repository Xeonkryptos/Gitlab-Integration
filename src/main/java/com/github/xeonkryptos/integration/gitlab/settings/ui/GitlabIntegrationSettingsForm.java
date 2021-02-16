package com.github.xeonkryptos.integration.gitlab.settings.ui;

import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService;
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings;
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import java.util.Map;
import javax.swing.JPanel;

/**
 * @author Xeonkryptos
 * @since 15.02.2021
 */
public class GitlabIntegrationSettingsForm {

    private final GitlabSettings settings;

    private final GitlabHostsTableModel gitlabHostsTableModel;

    private JPanel settingsPanel;
    private JBTable gitlabHostsTbl;

    public GitlabIntegrationSettingsForm(Project project) {
        settings = GitlabSettingsService.getInstance(project).getState();

        gitlabHostsTableModel = new GitlabHostsTableModel(settings);
        gitlabHostsTbl.setModel(gitlabHostsTableModel);
        ToolbarDecorator.createDecorator(gitlabHostsTbl);

        reset();
    }

    public boolean isModified() {
        Map<String, GitlabHostSettings> gitlabHostSettings = settings.getGitlabHostSettings();
        for (GitlabHostSettings gitlabHostUiSetting : gitlabHostsTableModel.getGitlabHostSettings()) {
            String gitlabHost = gitlabHostUiSetting.getGitlabHost();
            GitlabHostSettings storedGitlabHostSetting = gitlabHostSettings.get(gitlabHost);
            if (!storedGitlabHostSetting.equals(gitlabHostUiSetting)) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        gitlabHostsTableModel.apply();
    }

    public void reset() {
        gitlabHostsTableModel.reset();
    }

    public JPanel getSettingsPanel() {
        return settingsPanel;
    }
}
