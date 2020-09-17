package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.ui.component.TreeWithSearchComponent
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.intellij.ui.tree.TreePathUtil
import com.intellij.ui.treeStructure.Tree
import git4idea.remote.GitRememberedInputs
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Consumer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class CloneRepositoryUI(project: Project) {

    private val clonePathListeners = CopyOnWriteArraySet<Consumer<String?>>()

    private val searchField: SearchTextField
    private val tree: Tree

    private val treeModel: DefaultTreeModel

    val directoryField = SelectChildTextFieldWithBrowseButton(ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())).apply {
        val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fcd.isShowFileSystemRoots = true
        fcd.isHideIgnored = false
        addBrowseFolderListener(DvcsBundle.getString("clone.destination.directory.browser.title"), DvcsBundle.getString("clone.destination.directory.browser.description"), project, fcd)
    }

    val repositoryPanel: DialogPanel

    init {
        val treeRoot = DefaultMutableTreeNode()

        // TODO: Dummy test data. Remove later
        val xeonkryptosNode = DefaultMutableTreeNode("Xeonkryptos")
        xeonkryptosNode.add(DefaultMutableTreeNode("eclipse-project-creator"))
        treeRoot.add(xeonkryptosNode)
        treeRoot.add(DefaultMutableTreeNode("funartic"))

        treeModel = DefaultTreeModel(treeRoot)

        val treeWithSearchComponent = TreeWithSearchComponent(treeModel)
        tree = treeWithSearchComponent.tree.apply {
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

            addTreeSelectionListener {
                val selectionPath = selectionPath
                var selectedClonePath: String? = null
                if (selectionPath != null && (selectionPath.lastPathComponent as DefaultMutableTreeNode).isLeaf) {
                    selectedClonePath = TreePathUtil.convertTreePathToStrings(selectionPath).joinToString("/")
                }
                clonePathListeners.forEach { it.accept(selectedClonePath) }
            }
        }
        searchField = treeWithSearchComponent.searchField

        repositoryPanel = panel(LCFlags.fill) {
            row { searchField(growX, pushX) }
            row { ScrollPaneFactory.createScrollPane(tree)(grow, push) }
            row(GitlabBundle.message("clone.dialog.directory.field")) { directoryField(growX, pushX) }
        }
    }

    fun updateProjectList(projectList: List<GitlabProject>) {
        val parents = HashMap<String, DefaultMutableTreeNode>()
        projectList.forEach { gitlabProject ->
            if (!parents.containsKey(gitlabProject.name)) { // TODO: How to handle multi gitlab hosts with same project names? Prefix them with the received host?
                val projectPathEntriesCount = gitlabProject.name.count { it == '/' }
                if (projectPathEntriesCount == 1) {
                    val defaultMutableTreeNode = DefaultMutableTreeNode(gitlabProject.name)
                    parents[gitlabProject.name] = defaultMutableTreeNode
                    (treeModel.root as DefaultMutableTreeNode).add(defaultMutableTreeNode)
                } else if (projectPathEntriesCount > 1) {
                    addNodeIntoTree(gitlabProject.name, parents)
                }
            }
        }
    }

    private fun addNodeIntoTree(gitlabProjectName: String, parents: Map<String, DefaultMutableTreeNode>) {
        val parentName = gitlabProjectName.substringBeforeLast('/')
        val defaultMutableTreeNode = DefaultMutableTreeNode(gitlabProjectName)

        if (!parents.containsKey(parentName)) {
            addNodeIntoTree(gitlabProjectName, parents)
        }
        val parentNode = parents[parentName]
        parentNode!!.add(defaultMutableTreeNode)
        if (!parentName.contains('/')) {
            (treeModel.root as DefaultMutableTreeNode).add(defaultMutableTreeNode)
        }
    }

    fun addClonePathListener(clonePathListener: Consumer<String?>) {
        clonePathListeners.add(clonePathListener)
    }

    fun removeClonePathListener(clonePathListener: Consumer<String?>) {
        clonePathListeners.remove(clonePathListener)
    }
}
