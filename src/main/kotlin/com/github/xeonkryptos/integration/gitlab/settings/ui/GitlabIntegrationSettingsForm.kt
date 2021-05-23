package com.github.xeonkryptos.integration.gitlab.settings.ui

import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnActionButtonRunnable
import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import javax.swing.JPanel
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel

/**
 * @author Xeonkryptos
 * @since 15.02.2021
 */
class GitlabIntegrationSettingsForm(private val project: Project) : Disposable {

    private val gitlabHostsTableModel: GitlabHostsTableModel
    private val gitlabHostsTbl: JBTable

    val gitlabTablePanel: JPanel
    val isModified: Boolean
        get() = gitlabHostsTableModel.isModified()

    init {
        val settings = service<GitlabSettingsService>().state
        gitlabHostsTableModel = GitlabHostsTableModel(settings)
        Disposer.register(this, gitlabHostsTableModel)

        val gitlabHostColumn = TableColumn(0)
        gitlabHostColumn.headerValue = "Gitlab host"

        val gitlabSslVerificationColumn = TableColumn(1)
        gitlabSslVerificationColumn.headerValue = "Disable certificate verification"
        gitlabSslVerificationColumn.cellRenderer = BooleanTableCellRenderer()
        gitlabSslVerificationColumn.cellEditor = BooleanTableCellEditor()

        val gitlabUsernameColumn = TableColumn(2)
        gitlabUsernameColumn.headerValue = "Username"

        val gitlabResolveOwnProjectsColumn = TableColumn(3)
        gitlabResolveOwnProjectsColumn.headerValue = "Resolve only own projects"
        gitlabResolveOwnProjectsColumn.cellEditor = BooleanTableCellEditor()
        gitlabResolveOwnProjectsColumn.cellRenderer = BooleanTableCellRenderer()

        val tableColumnModel: TableColumnModel = DefaultTableColumnModel()
        tableColumnModel.addColumn(gitlabHostColumn)
        tableColumnModel.addColumn(gitlabSslVerificationColumn)
        tableColumnModel.addColumn(gitlabUsernameColumn)
        tableColumnModel.addColumn(gitlabResolveOwnProjectsColumn)

        gitlabHostsTbl = JBTable(gitlabHostsTableModel, tableColumnModel)
        gitlabHostsTbl.setShowGrid(false)

        val onAddAction = AnActionButtonRunnable {
            val addGitlabSettingsEntryDialog = AddGitlabSettingsEntryDialog(project)
            addGitlabSettingsEntryDialog.show()
        }
        val onRemoveAction = AnActionButtonRunnable {
            val selectedRow = gitlabHostsTbl.selectedRow
            gitlabHostsTableModel.removeEntry(selectedRow)
        }
        gitlabTablePanel = ToolbarDecorator.createDecorator(gitlabHostsTbl).setAddAction(onAddAction).setRemoveAction(onRemoveAction).createPanel()
    }

    fun apply() = gitlabHostsTableModel.apply()

    fun reset() = gitlabHostsTableModel.reset()

    override fun dispose() {}
}