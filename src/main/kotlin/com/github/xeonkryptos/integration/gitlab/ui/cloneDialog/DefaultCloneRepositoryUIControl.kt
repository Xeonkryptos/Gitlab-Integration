package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.api.UserProvider
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabAccountStateNotifier
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.ui.component.TreeNodeEntry
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.github.xeonkryptos.integration.gitlab.util.invokeOnDispatchThread
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ImageLoader
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import com.intellij.util.ui.cloneDialog.AccountMenuPopupStep
import com.intellij.util.ui.cloneDialog.AccountsMenuListPopup
import com.intellij.util.ui.cloneDialog.VcsCloneDialogUiSpec
import com.jetbrains.rd.util.firstOrNull
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.tree.DefaultMutableTreeNode

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
class DefaultCloneRepositoryUIControl(private val project: Project, ui: CloneRepositoryUI? = null, private val userProvider: UserProvider) : CloneRepositoryUIControl {

    private val applicationManager = ApplicationManager.getApplication()
    private val progressManager = ProgressManager.getInstance()

    private val gitlabApiManager = GitlabApiManager(project)
    private val gitlabSettings = GitlabSettingsService.getInstance(project).state
    private val authenticationManager = AuthenticationManager.getInstance(project)

    private val popupMenuMouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) = showPopupMenu()
    }

    override var ui: CloneRepositoryUI? = ui
        set(value) {
            field?.let {
                it.usersPanel.removeMouseListener(popupMenuMouseAdapter)
                if (it !== value) {
                    Disposer.dispose(it)
                }
            }
            field = value
            field?.let {
                it.usersPanel.addMouseListener(popupMenuMouseAdapter)
                initMessageListening(it)
            }
        }

    private fun showPopupMenu() {
        val menuItems = mutableListOf<AccountMenuItem>()

        for ((index, userEntry) in userProvider.getUsers().entries.withIndex()) {
            val accountTitle = userEntry.value.name ?: userEntry.value.username
            val serverInfo = userEntry.value.server.removePrefix("http://").removePrefix("https://")
            val avatar = ImageIcon(userEntry.value.avatar ?: ImageLoader.loadFromResource(GitlabUtil.GITLAB_ICON_PATH, javaClass))
            val accountActions = mutableListOf<AccountMenuItem.Action>()
            val showSeparatorAbove = index != 0

            accountActions += AccountMenuItem.Action(GitlabBundle.message("open.on.gitlab.action"), { BrowserUtil.browse(userEntry.value.server) }, AllIcons.Ide.External_link_arrow)
            if (!userEntry.key.signedIn) {
                accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.log.in"), {
                    if (!userEntry.key.signedIn) {
                        if (authenticationManager.hasAuthenticationTokenFor(userEntry.key)) {
                            userEntry.key.signedIn = true
                        } else {
                            // TODO: Make it possible to re-enter token. Keep in mind: Another token for another user might be added. Results in another account!
                        }
                    }
                }, showSeparatorAbove = true)
            }
            accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.log.out"), { userEntry.key.signedIn = false }, showSeparatorAbove = userEntry.key.signedIn)
            accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.delete"), {
                userEntry.key.delete()
                authenticationManager.deleteAuthenticationFor(userEntry.key)
            }, showSeparatorAbove = true)

            menuItems += AccountMenuItem.Account(accountTitle, serverInfo, avatar, accountActions, showSeparatorAbove)
        }
        // TODO: Add actions to login into a new account

        AccountsMenuListPopup(project, AccountMenuPopupStep(menuItems)).showUnderneathOf(ui!!.usersPanel)
    }

    private fun initMessageListening(ui: CloneRepositoryUI) {
        val connection = applicationManager.messageBus.connect(ui)
        connection.subscribe(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC, object : GitlabAccountStateNotifier {
            override fun onGitlabAccountCreated(gitlabAccount: GitlabAccount) {
                if (canAuthenticateAgainstGitlab(gitlabAccount)) {
                    reloadData()
                }
            }

            override fun onGitlabAccountDeleted(gitlabAccount: GitlabAccount) {
                val hasSignedInAccount = gitlabSettings.hasGitlabAccountBy { canAuthenticateAgainstGitlab(it) }
                if (hasSignedInAccount) {
                    ApplicationManager.getApplication().invokeOnDispatchThread(ui.repositoryPanel) { removeAccountProject(gitlabAccount) }
                }
            }
        })
        connection.subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, object : GitlabLoginChangeNotifier {
            override fun onSignIn(gitlabAccount: GitlabAccount) {
                if (!canAuthenticateAgainstGitlab(gitlabAccount)) {
                    ApplicationManager.getApplication().invokeOnDispatchThread(ui.repositoryPanel) { removeAccountProject(gitlabAccount) }
                }
            }

            override fun onSignOut(gitlabAccount: GitlabAccount) {
                val hasSignedInAccount = gitlabSettings.hasGitlabAccountBy { canAuthenticateAgainstGitlab(it) }
                if (hasSignedInAccount) {
                    ApplicationManager.getApplication().invokeOnDispatchThread(ui.repositoryPanel) { removeAccountProject(gitlabAccount) }
                }
            }
        })
    }

    override fun reloadData() {
        progressManager.runProcessWithProgressSynchronously({
                                                                val gitlabAccounts = gitlabSettings.getAllGitlabAccountsBy { authenticationManager.hasAuthenticationTokenFor(it) }
                                                                val gitlabUsersMap = gitlabApiManager.retrieveGitlabUsersFor(gitlabAccounts)

                                                                applicationManager.invokeLater {
                                                                    val firstUserEntry = gitlabUsersMap.firstOrNull()
                                                                    if (firstUserEntry != null) {
                                                                        addUserAccount(firstUserEntry.value)
                                                                    }
                                                                }

                                                                val gitlabProjectsMap = gitlabApiManager.retrieveGitlabProjectsFor(gitlabAccounts)
                                                                applicationManager.invokeLater {
                                                                    updateAccountProjects(gitlabProjectsMap)
                                                                    ui?.expandEntireTree()
                                                                }
                                                            }, "Loading repo data", true, project)
    }

    private fun canAuthenticateAgainstGitlab(gitlabAccount: GitlabAccount): Boolean = gitlabAccount.signedIn && authenticationManager.hasAuthenticationTokenFor(gitlabAccount)

    private fun addUserAccount(gitlabUser: GitlabUser?) {
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
        ui?.let {
            it.usersPanel.add(userLabel)
            it.usersPanel.invalidate()

            it.repositoryPanel.revalidate()
            it.repositoryPanel.repaint()
        }
    }

    private fun updateAccountProjects(accountProjects: Map<GitlabAccount, List<GitlabProject>>) {
        (ui?.model?.treeModel?.root as? DefaultMutableTreeNode)?.let { defaultMutableTreeNodeRoot ->
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
            repaintTree()
        }
    }

    fun removeAccountProject(gitlabAccount: GitlabAccount) {
        val gitlabTreeNodeName = createGitlabTreeNodeName(gitlabAccount)
        (ui?.model?.treeModel?.root as? DefaultMutableTreeNode)?.let { rootNode ->
            val childCount = rootNode.childCount
            for (i in 0..childCount) {
                val childAt = rootNode.getChildAt(i) as DefaultMutableTreeNode
                val treeNodeEntry = childAt.userObject as TreeNodeEntry
                if (treeNodeEntry.pathName == gitlabTreeNodeName) {
                    rootNode.remove(childAt)
                    break
                }
            }
        }
    }

    private fun repaintTree() {
        ui?.let {
            it.model.reload()
            it.tree.invalidate()

            it.repositoryPanel.revalidate()
            it.repositoryPanel.repaint()
        }
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

    override fun updateProjectName(projectName: String?) {
        ui?.directoryField?.trySetChildPath(projectName ?: "")
    }
}
