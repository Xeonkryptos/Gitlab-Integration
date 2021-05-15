package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.api.PagerProxy
import com.github.xeonkryptos.integration.gitlab.api.UserProvider
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabAccountStateNotifier
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.github.xeonkryptos.integration.gitlab.util.invokeOnDispatchThread
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ImageLoader
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.progress.ProgressVisibilityManager
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import com.intellij.util.ui.cloneDialog.AccountMenuPopupStep
import com.intellij.util.ui.cloneDialog.AccountsMenuListPopup
import com.intellij.util.ui.cloneDialog.VcsCloneDialogUiSpec
import com.jetbrains.rd.util.firstOrNull
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.ImageIcon
import javax.swing.JLabel

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
class DefaultCloneRepositoryUIControl(private val project: Project, ui: CloneRepositoryUI? = null, private val userProvider: UserProvider) : CloneRepositoryUIControl {

    private val applicationManager = ApplicationManager.getApplication()

    private val gitlabApiManager = GitlabApiManager(project)
    private val gitlabSettings = GitlabSettingsService.getInstance(project).state
    private val authenticationManager = AuthenticationManager.getInstance(project)

    @Volatile
    private var gitlabProjectsMap: Map<GitlabAccount, PagerProxy<List<GitlabProject>>>? = null

    private val progressIndicator: ProgressVisibilityManager = object : ProgressVisibilityManager() {
        override fun getModalityState(): ModalityState = ModalityState.any()

        override fun setProgressVisible(visible: Boolean) {
            this@DefaultCloneRepositoryUIControl.ui?.repositoryList?.setPaintBusy(visible)
        }
    }

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
            val accountTitle = userEntry.value.name
            val serverInfo = userEntry.value.server.removePrefix("http://").removePrefix("https://")
            val avatar = ImageIcon(userEntry.value.avatar ?: ImageLoader.loadFromResource(GitlabUtil.GITLAB_ICON_PATH, javaClass))
            val accountActions = mutableListOf<AccountMenuItem.Action>()
            val showSeparatorAbove = index != 0

            accountActions += AccountMenuItem.Action(GitlabBundle.message("open.on.gitlab.action"), { BrowserUtil.browse(userEntry.value.server) }, AllIcons.Ide.External_link_arrow)
            val signedIn: Boolean = authenticationManager.hasAuthenticationTokenFor(userEntry.key)
            if (!signedIn) {
                accountActions += AccountMenuItem.Action(
                    GitlabBundle.message("accounts.log.in"),
                    { // TODO: Make it possible to re-enter token. Keep in mind: A token is user-specific and a token for another account might be added. Thus, results in another account!
                    },
                    showSeparatorAbove = true
                )
            }
            accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.log.out"), {
                authenticationManager.deleteAuthenticationFor(userEntry.key)
            }, showSeparatorAbove = signedIn)
            accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.delete"), {
                userEntry.key.delete()
                authenticationManager.deleteAuthenticationFor(userEntry.key)
            }, showSeparatorAbove = true)

            menuItems += AccountMenuItem.Account(accountTitle, serverInfo, avatar, accountActions, showSeparatorAbove)
        } // TODO: Add actions to login into a new account

        AccountsMenuListPopup(project, AccountMenuPopupStep(menuItems)).showUnderneathOf(ui!!.usersPanel)
    }

    private fun initMessageListening(ui: CloneRepositoryUI) {
        val connection = applicationManager.messageBus.connect(ui)
        connection.subscribe(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC, object : GitlabAccountStateNotifier {
            override fun onGitlabAccountCreated(gitlabAccount: GitlabAccount) {
                if (authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
                    reloadData(gitlabAccount)
                }
            }

            override fun onGitlabAccountDeleted(gitlabAccount: GitlabAccount) {
                applicationManager.invokeOnDispatchThread(ui.repositoryPanel) { removeAccountProject(gitlabAccount) }
            }
        })
        connection.subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, object : GitlabLoginChangeNotifier {
            override fun onSignIn(gitlabAccount: GitlabAccount) {
                if (!authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
                    applicationManager.invokeOnDispatchThread(ui.repositoryPanel) { removeAccountProject(gitlabAccount) }
                } else {
                    reloadData(gitlabAccount)
                }
            }

            override fun onSignOut(gitlabAccount: GitlabAccount) {
                applicationManager.invokeOnDispatchThread(ui.repositoryPanel) { removeAccountProject(gitlabAccount) }
            }
        })
    }

    override fun reloadData(gitlabAccount: GitlabAccount?) {
        progressIndicator.run(object : Task.Backgroundable(project, "Repositories download", true, ALWAYS_BACKGROUND) {

            override fun run(indicator: ProgressIndicator) {
                val gitlabAccounts: List<GitlabAccount> = gitlabSettings.getAllGitlabAccountsBy { authenticationManager.hasAuthenticationTokenFor(it) }
                val gitlabUsersMap = gitlabApiManager.retrieveGitlabUsersFor(gitlabAccounts)

                applicationManager.invokeLater {
                    val firstUserEntry = gitlabUsersMap.firstOrNull()
                    if (firstUserEntry != null) {
                        addUserAccount(firstUserEntry.value)
                    }
                }

                gitlabProjectsMap = gitlabApiManager.retrieveGitlabProjectsFor(gitlabAccounts)
                applicationManager.invokeLater { updateAccountProjects() }
            }
        })
    }

    override fun hasPreviousRepositories(): Boolean {
        // Technically all have a previous page reference, not just one. It gets stream-lined by PagerProxy. For performance we're sticking to any
        return gitlabProjectsMap?.values?.any { it.hasPreviousPage() } ?: false;
    }

    override fun loadPreviousRepositories() {
        progressIndicator.run(object : Task.Backgroundable(project, "Previous repositories download", true, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                gitlabProjectsMap?.let {
                    it.values.forEach { pagerProxy -> pagerProxy.loadPreviousPage() }
                    applicationManager.invokeLater { updateAccountProjects() }
                }
            }
        })
    }

    override fun hasNextRepositories(): Boolean = gitlabProjectsMap?.values?.any { it.hasNextPage() } ?: false

    override fun loadNextRepositories() {
        progressIndicator.run(object : Task.Backgroundable(project, "Next repositories download", true, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                gitlabProjectsMap?.let {
                    it.values.forEach { pagerProxy -> pagerProxy.loadNextPage() }
                    applicationManager.invokeLater { updateAccountProjects() }
                }
            }
        })
    }

    @RequiresEdt
    private fun addUserAccount(gitlabUser: GitlabUser?) {
        ui?.let {
            if (it.usersPanel.components.isEmpty()) {
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
                it.usersPanel.add(userLabel)
                it.usersPanel.invalidate()

                it.repositoryPanel.revalidate()
                it.repositoryPanel.repaint()
            }
        }
    }

    @RequiresEdt
    private fun updateAccountProjects() {
        ui?.let { localUI ->
            localUI.repositoryListModel.removeAll()
            gitlabProjectsMap?.entries?.mapNotNull { it.value.currentData?.map { gitlabProject -> GitlabProjectListItem(it.key, gitlabProject) } }
                ?.forEach { pagerData -> ui?.repositoryListModel?.add(pagerData) }
        }
    }

    @RequiresEdt
    private fun removeAccountProject(gitlabAccount: GitlabAccount) = ui?.repositoryListModel?.removeIf { it.gitlabAccount == gitlabAccount }

    @RequiresEdt
    override fun updateProjectName(projectName: String?) {
        ui?.directoryField?.trySetChildPath(projectName ?: "")
    }

    override fun getAvailableAccounts(): Collection<GitlabAccount> = gitlabProjectsMap?.keys ?: Collections.emptyList()
}
