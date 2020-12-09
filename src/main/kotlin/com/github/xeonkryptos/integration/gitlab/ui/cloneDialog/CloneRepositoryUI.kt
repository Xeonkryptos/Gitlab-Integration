package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProjectWrapper
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
        addBrowseFolderListener(DvcsBundle.message("clone.destination.directory.browser.title"), DvcsBundle.message("clone.destination.directory.browser.description"), project, fcd)
    }

    val repositoryPanel: DialogPanel

    init {
        val treeRoot = DefaultMutableTreeNode()
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

    fun updateProjectList(projectList: List<GitlabProjectWrapper>) {
        val defaultMutableTreeNodeRoot = treeModel.root as DefaultMutableTreeNode
        defaultMutableTreeNodeRoot.removeAllChildren()
        treeModel.nodeStructureChanged(defaultMutableTreeNodeRoot)

        val parents = HashMap<String, DefaultMutableTreeNode>()
        projectList.forEach { gitlabProject ->
            val projectNameWithNamespace = gitlabProject.project.nameWithNamespace.replace(" / ", "/")
            if (!parents.containsKey(projectNameWithNamespace)) {
                val projectPathEntriesCount = projectNameWithNamespace.count { it == '/' }
                if (projectPathEntriesCount == 0) {
                    val defaultMutableTreeNode = DefaultMutableTreeNode(projectNameWithNamespace)
                    parents[projectNameWithNamespace] = defaultMutableTreeNode
                    defaultMutableTreeNodeRoot.add(defaultMutableTreeNode)
                } else if (projectPathEntriesCount >= 1) {
                    addNodeIntoTree(projectNameWithNamespace, parents)
                }
            }
        }

        treeModel.reload()
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
