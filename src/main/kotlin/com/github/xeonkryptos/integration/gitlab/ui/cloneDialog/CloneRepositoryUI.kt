package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProjectWrapper
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.ui.component.TreeNodeEntry
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
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.ImageUtil
import git4idea.remote.GitRememberedInputs
import java.awt.image.BufferedImage
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Consumer
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JSeparator
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class CloneRepositoryUI(project: Project) {

    private companion object {
        private const val AVATAR_IMAGE_SIZE_WITH_AND_HEIGHT = 24
    }

    private val clonePathListeners = CopyOnWriteArraySet<Consumer<GitlabProjectWrapper?>>()

    private val searchField: SearchTextField
    private val tree: Tree

    private val treeModel: DefaultTreeModel

    private val avatarLabel = JLabel()

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
                var selectedGitlabProject: GitlabProjectWrapper? = null
                if (selectionPath != null && (selectionPath.lastPathComponent as DefaultMutableTreeNode).isLeaf) {
                    selectedGitlabProject = ((selectionPath.lastPathComponent as DefaultMutableTreeNode).userObject as TreeNodeEntry).gitlabProject
                }
                clonePathListeners.forEach { it.accept(selectedGitlabProject) }
            }
        }
        searchField = treeWithSearchComponent.searchField

        val avatarImageSeparator = JSeparator()
        repositoryPanel = panel(LCFlags.fill) {
            row {
                cell(isFullWidth = true) {
                    searchField(growX, pushX)
                    avatarImageSeparator()
                    avatarLabel()
                }
            }
            row { ScrollPaneFactory.createScrollPane(tree)(grow, push) }
            row(GitlabBundle.message("clone.dialog.directory.field")) { directoryField(growX, pushX) }
        }
    }

    fun updateUserAvatar(avatarImage: BufferedImage?) {
        if (avatarImage != null) {
            val scaledImage = ImageUtil.scaleImage(avatarImage, AVATAR_IMAGE_SIZE_WITH_AND_HEIGHT, AVATAR_IMAGE_SIZE_WITH_AND_HEIGHT)
            avatarLabel.icon = ImageIcon(scaledImage)
        }
    }

    fun updateProjectList(projectList: List<GitlabProjectWrapper>) {
        val defaultMutableTreeNodeRoot = treeModel.root as DefaultMutableTreeNode
        defaultMutableTreeNodeRoot.removeAllChildren()

        val parents = HashMap<String, DefaultMutableTreeNode>()
        projectList.forEach { gitlabProject ->
            val projectNameWithNamespace = gitlabProject.viewableProjectPath
            if (!parents.containsKey(projectNameWithNamespace)) {
                val projectPathEntriesCount = projectNameWithNamespace.count { it == '/' }
                if (projectPathEntriesCount == 0) {
                    val gitlabProjectTreeNode = DefaultMutableTreeNode(TreeNodeEntry(projectNameWithNamespace, gitlabProject))
                    parents[projectNameWithNamespace] = gitlabProjectTreeNode
                    defaultMutableTreeNodeRoot.add(gitlabProjectTreeNode)
                } else if (projectPathEntriesCount >= 1) {
                    addNodeIntoTree(projectNameWithNamespace, gitlabProject, parents)
                }
            }
        }

        treeModel.reload()
    }

    private fun addNodeIntoTree(gitlabProjectPath: String, gitlabProject: GitlabProjectWrapper, parents: MutableMap<String, DefaultMutableTreeNode>) {
        val parentName = gitlabProjectPath.substringBeforeLast('/')
        if (!parents.containsKey(parentName) && parentName.contains('/')) {
            addNodeIntoTree(parentName, gitlabProject, parents)
        }
        if (!parents.containsKey(parentName) && !parentName.contains('/')) {
            parents[parentName] = DefaultMutableTreeNode(TreeNodeEntry(parentName))
            (treeModel.root as DefaultMutableTreeNode).add(parents[parentName])
        }
        if (parents.containsKey(parentName)) {
            val currentProjectName = gitlabProjectPath.substringAfterLast('/')
            val treeNodeEntry = TreeNodeEntry(currentProjectName)
            val childNode = DefaultMutableTreeNode(treeNodeEntry)
            if (gitlabProjectPath == gitlabProject.viewableProjectPath) {
                treeNodeEntry.gitlabProject = gitlabProject
            }
            parents[parentName]?.add(childNode)
            parents[gitlabProjectPath] = childNode
        }
    }

    fun updateProjectName(projectName: String?) {
        directoryField.trySetChildPath(projectName ?: "")
    }

    fun addClonePathListener(clonePathListener: Consumer<GitlabProjectWrapper?>) {
        clonePathListeners.add(clonePathListener)
    }

    fun removeClonePathListener(clonePathListener: Consumer<GitlabProjectWrapper?>) {
        clonePathListeners.remove(clonePathListener)
    }
}
