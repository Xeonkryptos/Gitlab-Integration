package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.UserProvider
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.ui.component.TreeNodeEntry
import com.github.xeonkryptos.integration.gitlab.ui.component.TreeWithSearchComponent
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.SearchTextField
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.JBUI
import git4idea.remote.GitRememberedInputs
import java.awt.FlowLayout
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Consumer
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class CloneRepositoryUI(private val project: Project, userProvider: UserProvider, val model: CloneRepositoryUIModel = DefaultCloneRepositoryUIModel()) : Disposable {

    private val clonePathListeners = CopyOnWriteArraySet<Consumer<GitlabProject?>>()

    private val searchField: SearchTextField

    internal val tree: Tree
    internal val usersPanel: JPanel = JPanel(FlowLayout(FlowLayout.LEADING, JBUI.scale(1), 0))

    val directoryField = SelectChildTextFieldWithBrowseButton(ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())).apply {
        val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fcd.isShowFileSystemRoots = true
        fcd.isHideIgnored = false
        addBrowseFolderListener(DvcsBundle.message("clone.destination.directory.browser.title"), DvcsBundle.message("clone.destination.directory.browser.description"), project, fcd)
    }

    val repositoryPanel: JPanel
    var controller: CloneRepositoryUIControl = DefaultCloneRepositoryUIControl(project, this, userProvider)

    init {
        val treeWithSearchComponent = TreeWithSearchComponent(model.treeModel)
        tree = treeWithSearchComponent.tree.apply {
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

            addTreeSelectionListener {
                val selectionPath = selectionPath
                var selectedGitlabProject: GitlabProject? = null
                if (selectionPath != null && (selectionPath.lastPathComponent as DefaultMutableTreeNode).isLeaf) {
                    selectedGitlabProject = ((selectionPath.lastPathComponent as DefaultMutableTreeNode).userObject as TreeNodeEntry).gitlabProject
                }
                clonePathListeners.forEach { it.accept(selectedGitlabProject) }
            }
        }
        searchField = treeWithSearchComponent.searchField
        val treePanel = ToolbarDecorator.createDecorator(tree)
                .setMoveUpAction { controller.loadPreviousRepositories() }
                .setMoveUpActionUpdater { controller.hasPreviousRepositories() }
                .setMoveDownAction { controller.loadNextRepositories() }
                .setMoveDownActionUpdater { controller.hasNextRepositories() }
                .createPanel()

        repositoryPanel = panel(LCFlags.fill) {
            row {
                cell(isFullWidth = true) {
                    searchField(growX, pushX)
                    JSeparator(JSeparator.VERTICAL)(growY).withLargeLeftGap()
                    usersPanel().withLargeLeftGap()
                }
            }
            row { treePanel(grow, push) }
            row(GitlabBundle.message("clone.dialog.directory.field")) { directoryField(growX, pushX) }
        }

        controller.ui = this
    }

    @RequiresEdt
    fun expandEntireTree() {
        var index = 0
        while (index < tree.rowCount) {
            tree.expandRow(index++)
        }
    }

    fun reloadData() {
        controller.reloadData()
    }

    override fun dispose() {}

    fun addClonePathListener(clonePathListener: Consumer<GitlabProject?>) {
        clonePathListeners.add(clonePathListener)
    }
}
