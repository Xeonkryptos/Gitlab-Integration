package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.UserProvider
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.ui.component.TreeNodeEntry
import com.github.xeonkryptos.integration.gitlab.ui.component.TreeWithSearchComponent
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ImageLoader
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import com.intellij.util.ui.cloneDialog.AccountMenuPopupStep
import com.intellij.util.ui.cloneDialog.AccountsMenuListPopup
import com.intellij.util.ui.cloneDialog.VcsCloneDialogUiSpec
import git4idea.remote.GitRememberedInputs
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Consumer
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class CloneRepositoryUI(private val project: Project, private val userProvider: UserProvider) {

    private val authenticationManager = AuthenticationManager.getInstance(project)
    private val gitlabDataService = GitlabDataService.getInstance(project)

    private val clonePathListeners = CopyOnWriteArraySet<Consumer<GitlabProject?>>()

    private val searchField: SearchTextField
    private val tree: Tree

    private val treeModel: DefaultTreeModel

    private val popupMenuMouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) = showPopupMenu()
    }
    private val usersPanel: JPanel = JPanel(FlowLayout(FlowLayout.LEADING, JBUI.scale(1), 0)).apply {
        addMouseListener(popupMenuMouseAdapter)
    }

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
                var selectedGitlabProject: GitlabProject? = null
                if (selectionPath != null && (selectionPath.lastPathComponent as DefaultMutableTreeNode).isLeaf) {
                    selectedGitlabProject = ((selectionPath.lastPathComponent as DefaultMutableTreeNode).userObject as TreeNodeEntry).gitlabProject
                }
                clonePathListeners.forEach { it.accept(selectedGitlabProject) }
            }
        }
        searchField = treeWithSearchComponent.searchField

        repositoryPanel = panel(LCFlags.fill) {
            row {
                cell(isFullWidth = true) {
                    searchField(growX, pushX)
                    JSeparator(JSeparator.VERTICAL)(growY).withLargeLeftGap()
                    usersPanel().withLargeLeftGap()
                }
            }
            row { ScrollPaneFactory.createScrollPane(tree)(grow, push) }
            row(GitlabBundle.message("clone.dialog.directory.field")) { directoryField(growX, pushX) }
        }
    }

    private fun showPopupMenu() {
        val menuItems = mutableListOf<AccountMenuItem>()

        for ((index, user) in userProvider.getUsers().withIndex()) {
            val accountTitle = user.name ?: user.username
            val serverInfo = user.server.removePrefix("http://").removePrefix("https://")
            val avatar = ImageIcon(user.avatar ?: ImageLoader.loadFromResource(GitlabUtil.GITLAB_ICON_PATH, javaClass))
            val accountActions = mutableListOf<AccountMenuItem.Action>()
            val showSeparatorAbove = index != 0

            accountActions += AccountMenuItem.Action(GitlabBundle.message("open.on.gitlab.action"), { BrowserUtil.browse(user.server) }, AllIcons.Ide.External_link_arrow)
            if (!user.gitlabAccount.signedIn) {
                accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.log.in"), {
                    if (!user.gitlabAccount.signedIn) {
                        if (authenticationManager.hasAuthenticationTokenFor(user.gitlabAccount)) {
                            user.gitlabAccount.signedIn = true
                        } else {
                            // TODO: Make it possible to re-enter token. Keep in mind: Another token for another user might be added. Results in another account!
                        }
                    }
                }, showSeparatorAbove = true)
            }
            accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.log.out"), { gitlabDataService.state.signOut(user.gitlabAccount) }, showSeparatorAbove = user.gitlabAccount.signedIn)
            accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.delete"), {
                gitlabDataService.state.removeGitlabAccount(user.gitlabAccount)
                authenticationManager.deleteAuthenticationFor(user.gitlabAccount)
            }, showSeparatorAbove = true)

            menuItems += AccountMenuItem.Account(accountTitle, serverInfo, avatar, accountActions, showSeparatorAbove)
        }

        AccountsMenuListPopup(project, AccountMenuPopupStep(menuItems)).showUnderneathOf(usersPanel)
    }

    fun addUserAccount(gitlabUser: GitlabUser?) {
        var avatar = gitlabUser?.avatar
        avatar = if (avatar != null) {
            ImageUtil.scaleImage(avatar, VcsCloneDialogUiSpec.Components.avatarSize, VcsCloneDialogUiSpec.Components.avatarSize)
        } else {
            val defaultAvatarImage = ImageLoader.loadFromResource(GitlabUtil.GITLAB_ICON_PATH, javaClass)
            ImageUtil.scaleImage(defaultAvatarImage, VcsCloneDialogUiSpec.Components.avatarSize, VcsCloneDialogUiSpec.Components.avatarSize)
        }
        val userLabel = JLabel().apply {
            icon = ImageIcon(avatar)
            toolTipText = gitlabUser?.username
            isOpaque = false
            addMouseListener(popupMenuMouseAdapter)
        }
        usersPanel.add(userLabel)
        usersPanel.invalidate()

        repositoryPanel.revalidate()
        repositoryPanel.repaint()
    }

    fun updateAccountProjects(accountProjects: Map<GitlabAccount, List<GitlabProject>>) {
        val defaultMutableTreeNodeRoot = treeModel.root as DefaultMutableTreeNode
        defaultMutableTreeNodeRoot.removeAllChildren()

        val parents = mutableMapOf<String, DefaultMutableTreeNode>()
        accountProjects.forEach { (gitlabAccount, gitlabProjects) ->
            val gitlabAccountRootNodeName = createGitlabTreeNodeName(gitlabAccount)
            val hostTreeNode = parents.computeIfAbsent(gitlabAccountRootNodeName) {
                val treeNode = DefaultMutableTreeNode(TreeNodeEntry(it))
                defaultMutableTreeNodeRoot.add(treeNode)
                return@computeIfAbsent treeNode
            }

            gitlabProjects.forEach { gitlabProject ->
                val projectNameWithNamespace = gitlabProject.viewableProjectPath
                if (!parents.containsKey(projectNameWithNamespace)) {
                    val projectPathEntriesCount = projectNameWithNamespace.count { it == '/' }
                    if (projectPathEntriesCount == 0) {
                        val gitlabProjectTreeNode = DefaultMutableTreeNode(TreeNodeEntry(projectNameWithNamespace, gitlabProject))
                        parents[projectNameWithNamespace] = gitlabProjectTreeNode
                        hostTreeNode.add(gitlabProjectTreeNode)
                    } else if (projectPathEntriesCount >= 1) {
                        addNodeIntoTree(projectNameWithNamespace, gitlabProject, parents)
                    }
                }
            }
        }

        treeModel.reload()
        tree.invalidate()

        repositoryPanel.revalidate()
        repositoryPanel.repaint()
    }

    fun removeAccountProject(gitlabAccount: GitlabAccount) {
        val gitlabTreeNodeName = createGitlabTreeNodeName(gitlabAccount)
        val rootNode = treeModel.root as DefaultMutableTreeNode
        val childCount = rootNode.childCount
        for (i in 0..childCount) {
            val childAt = rootNode.getChildAt(i) as DefaultMutableTreeNode
            val treeNodeEntry = childAt.userObject as TreeNodeEntry
            if (treeNodeEntry.pathName == gitlabTreeNodeName) {
                rootNode.remove(childAt)
                break
            }
        }
        treeModel.reload()
        tree.invalidate()

        repositoryPanel.revalidate()
        repositoryPanel.repaint()
    }

    private fun addNodeIntoTree(gitlabProjectPath: String, gitlabProject: GitlabProject, parents: MutableMap<String, DefaultMutableTreeNode>) {
        val parentName = gitlabProjectPath.substringBeforeLast('/')
        if (!parents.containsKey(parentName) && parentName.contains('/')) {
            addNodeIntoTree(parentName, gitlabProject, parents)
        }
        if (!parents.containsKey(parentName) && !parentName.contains('/')) {
            parents[parentName] = DefaultMutableTreeNode(TreeNodeEntry(parentName))
            val gitlabTreeNodeName = createGitlabTreeNodeName(gitlabProject.gitlabAccount)
            (parents[gitlabTreeNodeName] as DefaultMutableTreeNode).add(parents[parentName])
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

    private fun createGitlabTreeNodeName(gitlabAccount: GitlabAccount) = "${gitlabAccount.getNormalizeGitlabHost()} (${gitlabAccount.username})"

    fun updateProjectName(projectName: String?) {
        directoryField.trySetChildPath(projectName ?: "")
    }

    fun addClonePathListener(clonePathListener: Consumer<GitlabProject?>) {
        clonePathListeners.add(clonePathListener)
    }
}
