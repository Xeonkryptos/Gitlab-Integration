package com.github.xeonkryptos.integration.gitlab.settings.ui;

import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService;
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * @author Xeonkryptos
 * @since 15.02.2021
 */
public class GitlabIntegrationSettingsForm {

    private final GitlabHostsTableModel gitlabHostsTableModel;

    private JBTable gitlabHostsTbl;
    private JPanel settingsPanel;
    @SuppressWarnings("unused")
    private JPanel gitlabTablePanel;

    public GitlabIntegrationSettingsForm(Project project) {
        GitlabSettings settings = GitlabSettingsService.getInstance(project).getState();
        gitlabHostsTableModel = new GitlabHostsTableModel(settings);
        gitlabHostsTbl.setModel(gitlabHostsTableModel);
    }

    public boolean isModified() {
        return gitlabHostsTableModel.isModified();
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

    private void createUIComponents() {
        TableColumn gitlabHostColumn = new TableColumn(0);
        gitlabHostColumn.setHeaderValue("Gitlab host");

        TableColumn gitlabSslVerificationColumn = new TableColumn(1);
        gitlabSslVerificationColumn.setHeaderValue("Disable certificate verification");
        gitlabSslVerificationColumn.setCellRenderer(new BooleanTableCellRenderer());
        gitlabSslVerificationColumn.setCellEditor(new BooleanTableCellEditor());

        TableColumn gitlabUsernameColumn = new TableColumn(2);
        gitlabUsernameColumn.setHeaderValue("Username");

        TableColumn gitlabResolveOwnProjectsColumn = new TableColumn(3);
        gitlabResolveOwnProjectsColumn.setHeaderValue("Resolve only own projects");
        gitlabResolveOwnProjectsColumn.setCellEditor(new BooleanTableCellEditor());
        gitlabResolveOwnProjectsColumn.setCellRenderer(new BooleanTableCellRenderer());

        TableColumnModel tableColumnModel = new DefaultTableColumnModel();
        tableColumnModel.addColumn(gitlabHostColumn);
        tableColumnModel.addColumn(gitlabSslVerificationColumn);
        tableColumnModel.addColumn(gitlabUsernameColumn);
        tableColumnModel.addColumn(gitlabResolveOwnProjectsColumn);

        gitlabHostsTbl = new JBTable(null, tableColumnModel);
        gitlabHostsTbl.setShowGrid(false);

        AnActionButtonRunnable onAddAction = anActionButton -> {
        };
        AnActionButtonRunnable onEditAction = anActionButton -> {
        };
        AnActionButtonRunnable onRemoveAction = anActionButton -> {
        };

        gitlabTablePanel = ToolbarDecorator.createDecorator(gitlabHostsTbl).setAddAction(onAddAction).setEditAction(onEditAction).setRemoveAction(onRemoveAction).createPanel();
    }
}
