package com.github.xeonkryptos.integration.gitlab.ui.clone.repository

import com.github.xeonkryptos.integration.gitlab.api.PagerProxy
import com.github.xeonkryptos.integration.gitlab.api.gitlab.GitlabProjectsApi
import com.github.xeonkryptos.integration.gitlab.api.gitlab.GitlabUserApi
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabAccountStateNotifier
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.ui.clone.repository.event.ClonePathEvent
import com.github.xeonkryptos.integration.gitlab.ui.clone.repository.event.ClonePathEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.GlobalSearchTextEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.GlobalSearchTextEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.PagingEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.PagingEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.ReloadDataEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.ReloadDataEventListener
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotificationIdsHolder
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
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
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.SwingUtilities

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
class CloneRepositoryUIControl(private val project: Project, val ui: CloneRepositoryUI) {

    private companion object {
        val LOG = GitlabUtil.LOG
    }

    private val gitlabUserApi = service<GitlabUserApi>()
    private val gitlabProjectsApi = service<GitlabProjectsApi>()

    private val gitlabSettings = service<GitlabSettingsService>().getWorkableState()
    private val authenticationManager = service<AuthenticationManager>()

    @Volatile
    private var gitlabProjectsMap: Map<GitlabAccount, PagerProxy<List<GitlabProject>>>? = null

    private var filtered: Boolean = false

    private val progressIndicator: ProgressVisibilityManager = object : ProgressVisibilityManager() {
        override fun getModalityState(): ModalityState = ModalityState.any()

        override fun setProgressVisible(visible: Boolean) {
            this@CloneRepositoryUIControl.ui.gitlabProjectItemsList.setPaintBusy(visible)
        }
    }

    private val popupMenuMouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) = showPopupMenu()
    }

    fun registerBehaviourListeners() {
        ui.usersPanel.addMouseListener(popupMenuMouseAdapter)
        initMessageListening(ui)
        ui.addGlobalSearchTextEventListener(object : GlobalSearchTextEventListener {
            override fun onGlobalSearchTextChanged(event: GlobalSearchTextEvent) {
                doGlobalSearch(event.globalSearchText)
            }

            override fun onGlobalSearchTextDeleted(event: GlobalSearchTextEvent) {
                if (filtered) doGlobalSearch(null)
            }
        })
        ui.addReloadDataEventListener(object : ReloadDataEventListener {
            override fun onReloadRequest(event: ReloadDataEvent) {
                reloadData()
            }
        })
        ui.addClonePathEventListener(object : ClonePathEventListener {
            override fun onClonePathChanged(event: ClonePathEvent) {
                ui.directoryField.trySetChildPath(event.gitlabProject?.viewableProjectPath?.substringAfterLast("/") ?: "")
            }
        })
        ui.addPagingEventListener(object : PagingEventListener {
            override fun onPreviousPage(event: PagingEvent) {
                loadPreviousRepositories()
            }

            override fun onNextPage(event: PagingEvent) {
                loadNextRepositories()
            }
        })
    }

    private fun showPopupMenu() {
        val menuItems = mutableListOf<AccountMenuItem>()

        val settingsService = service<GitlabSettingsService>()
        val gitlabUsersPerAccount = service<GitlabUserApi>().retrieveGitlabUsersFor(project, settingsService.getWorkableState().getAllGitlabAccounts())
        for ((index, userEntry) in gitlabUsersPerAccount.entries.withIndex()) {
            val accountTitle = userEntry.value.name

            val avatar = ImageIcon(userEntry.value.avatar ?: ImageLoader.loadFromResource(GitlabUtil.GITLAB_ICON_PATH, javaClass))
            val accountActions = mutableListOf<AccountMenuItem.Action>()
            val showSeparatorAbove = index != 0

            accountActions += AccountMenuItem.Action(GitlabBundle.message("open.on.gitlab.action"), { BrowserUtil.browse(userEntry.value.server) }, AllIcons.Ide.External_link_arrow)
            val signedIn: Boolean = authenticationManager.hasAuthenticationTokenFor(userEntry.key)
            if (!signedIn) {
                accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.log.in"),
                                                         { // TODO: Make it possible to re-enter token. Keep in mind: A token is user-specific and a token for another account might be added. Thus, results in another account!
                                                         },
                                                         showSeparatorAbove = true)
            }
            accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.log.out"), {
                authenticationManager.deleteAuthenticationFor(userEntry.key)
            }, showSeparatorAbove = signedIn)
            accountActions += AccountMenuItem.Action(GitlabBundle.message("accounts.delete"), {
                authenticationManager.deleteAuthenticationFor(userEntry.key)
            }, showSeparatorAbove = true)

            menuItems += AccountMenuItem.Account(accountTitle, userEntry.value.server, avatar, accountActions, showSeparatorAbove)
        } // TODO: Add actions to login into a new account

        AccountsMenuListPopup(project, AccountMenuPopupStep(menuItems)).showUnderneathOf(ui.usersPanel)
    }

    private fun initMessageListening(ui: CloneRepositoryUI) {
        val connection = ApplicationManager.getApplication().messageBus.connect(ui)
        connection.subscribe(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC, object : GitlabAccountStateNotifier {
            override fun onGitlabAccountCreated(gitlabAccount: GitlabAccount) {
                if (authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
                    reloadData(gitlabAccount)
                }
            }

            override fun onGitlabAccountDeleted(gitlabAccount: GitlabAccount) {
                SwingUtilities.invokeLater { removeAccountProject(gitlabAccount) }
            }
        })
        connection.subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, object : GitlabLoginChangeNotifier {
            override fun onSignIn(gitlabAccount: GitlabAccount) {
                if (!authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
                    SwingUtilities.invokeLater { removeAccountProject(gitlabAccount) }
                } else {
                    reloadData(gitlabAccount)
                }
            }

            override fun onSignOut(gitlabAccount: GitlabAccount) {
                SwingUtilities.invokeLater { removeAccountProject(gitlabAccount) }
            }
        })
    }

    fun reloadData(gitlabAccount: GitlabAccount? = null) { // TODO: Implement feature to reload data only for a provided account
        filtered = false
        progressIndicator.run(object : Task.Backgroundable(project, "Repositories download", true, ALWAYS_BACKGROUND) {

            override fun run(indicator: ProgressIndicator) {
                val gitlabAccounts = loadAccounts()
                val localGitlabProjectsMap = gitlabProjectsApi.retrieveGitlabProjectsFor(project, gitlabAccounts)
                gitlabProjectsMap = localGitlabProjectsMap
                updatePagingPointers(localGitlabProjectsMap.values)

                ui.repositoryModel.availableAccounts = gitlabAccounts
                SwingUtilities.invokeLater { updateAccountProjects() }
            }
        })
    }

    private fun doGlobalSearch(globalSearchText: String?) {
        filtered = globalSearchText != null && globalSearchText.isNotBlank()
        progressIndicator.run(object : Task.Backgroundable(project, "Filtered repositories download", true, ALWAYS_BACKGROUND) {

            override fun run(indicator: ProgressIndicator) {
                val gitlabAccounts: Collection<GitlabAccount> = gitlabProjectsMap?.keys ?: loadAccounts()
                val localGitlabProjectsMap = gitlabProjectsApi.retrieveGitlabProjectsFor(project, gitlabAccounts, globalSearchText)
                gitlabProjectsMap = localGitlabProjectsMap
                updatePagingPointers(localGitlabProjectsMap.values)

                ui.repositoryModel.availableAccounts = gitlabAccounts
                SwingUtilities.invokeLater { updateAccountProjects() }
            }
        })
    }

    private fun loadAccounts(): List<GitlabAccount> {
        val gitlabAccounts: List<GitlabAccount> = gitlabSettings.getAllGitlabAccountsBy { authenticationManager.hasAuthenticationTokenFor(it) }
        val gitlabUsersMap = gitlabUserApi.retrieveGitlabUsersFor(project, gitlabAccounts)

        SwingUtilities.invokeLater {
            val firstUserEntry = gitlabUsersMap.firstOrNull()
            if (firstUserEntry != null) {
                addUserAccount(firstUserEntry.value)
            }
        }
        return gitlabAccounts
    }

    private fun loadPreviousRepositories() {
        progressIndicator.run(object : Task.Backgroundable(project, "Previous repositories download", true, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                gitlabProjectsMap?.let {
                    it.values.forEach { pagerProxy ->
                        try {
                            pagerProxy.loadPreviousPage()
                        } catch (e: Exception) {
                            GitlabNotifications.showError(project, GitlabNotificationIdsHolder.LOAD_GITLAB_ACCOUNTS_FAILED, GitlabBundle.message("load.gitlab.projects.failed", e), e)
                            LOG.warn(e)
                        }
                    }
                    updatePagingPointers(it.values)
                    SwingUtilities.invokeLater { updateAccountProjects() }
                }
            }
        })
    }

    private fun loadNextRepositories() {
        progressIndicator.run(object : Task.Backgroundable(project, "Next repositories download", true, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                gitlabProjectsMap?.let {
                    it.values.forEach { pagerProxy ->
                        try {
                            pagerProxy.loadNextPage()
                        } catch (e: Exception) {
                            GitlabNotifications.showError(project, GitlabNotificationIdsHolder.LOAD_GITLAB_ACCOUNTS_FAILED, GitlabBundle.message("load.gitlab.projects.failed", e), e)
                            LOG.warn(e)
                        }
                    }
                    updatePagingPointers(it.values)
                    SwingUtilities.invokeLater { updateAccountProjects() }
                }
            }
        })
    }

    private fun updatePagingPointers(pagerProxies: Collection<PagerProxy<List<GitlabProject>>>) {
        ui.repositoryModel.hasPreviousRepositories = pagerProxies.any { pager -> pager.hasPreviousPage() }
        ui.repositoryModel.hasNextRepositories = pagerProxies.any { pager -> pager.hasNextPage() }
    }

    @RequiresEdt
    private fun addUserAccount(gitlabUser: GitlabUser?) {
        if (ui.usersPanel.components.isEmpty()) {
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
            ui.usersPanel.add(userLabel)
            ui.usersPanel.invalidate()

            ui.repositoryPanel.revalidate()
            ui.repositoryPanel.repaint()
        }
    }

    @RequiresEdt
    private fun updateAccountProjects() {
        ui.repositoryModel.removeAll()
        gitlabProjectsMap?.entries?.mapNotNull { it.value.currentData?.map { gitlabProject -> GitlabProjectListItem(it.key, gitlabProject) } }
            ?.forEach { pagerData -> ui.repositoryModel.add(pagerData) }
    }

    @RequiresEdt
    private fun removeAccountProject(gitlabAccount: GitlabAccount) = ui.repositoryModel.removeIf { it.gitlabAccount == gitlabAccount }
}
