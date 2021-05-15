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
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ClonePathEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ClonePathEventListener
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.GlobalSearchTextEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.GlobalSearchTextEventListener
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.PagingEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.PagingEventListener
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ReloadDataEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ReloadDataEventListener
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.github.xeonkryptos.integration.gitlab.util.invokeOnDispatchThread
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.util.ImageLoader
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.progress.ProgressVisibilityManager
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import com.intellij.util.ui.cloneDialog.AccountMenuPopupStep
import com.intellij.util.ui.cloneDialog.AccountsMenuListPopup
import com.intellij.util.ui.cloneDialog.VcsCloneDialogUiSpec
import com.jetbrains.rd.util.firstOrNull
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.event.DocumentEvent

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
class DefaultCloneRepositoryUIControl(private val project: Project, val ui: CloneRepositoryUI, private val userProvider: UserProvider) : CloneRepositoryUIControl {

    private val applicationManager = ApplicationManager.getApplication()

    private val gitlabApiManager = GitlabApiManager(project)
    private val gitlabSettings = GitlabSettingsService.getInstance(project).state
    private val authenticationManager = AuthenticationManager.getInstance(project)

    @Volatile
    private var gitlabProjectsMap: Map<GitlabAccount, PagerProxy<List<GitlabProject>>>? = null

    private var filtered: Boolean = false

    private val progressIndicator: ProgressVisibilityManager = object : ProgressVisibilityManager() {
        override fun getModalityState(): ModalityState = ModalityState.any()

        override fun setProgressVisible(visible: Boolean) {
            this@DefaultCloneRepositoryUIControl.ui.listWithSearchComponent.list.setPaintBusy(visible)
        }
    }

    private val popupMenuMouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) = showPopupMenu()
    }

    override fun registerBehaviourListeners() {
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

        for ((index, userEntry) in userProvider.getUsers().entries.withIndex()) {
            val accountTitle = userEntry.value.name

            @Suppress("HttpUrlsUsage")
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

        AccountsMenuListPopup(project, AccountMenuPopupStep(menuItems)).showUnderneathOf(ui.usersPanel)
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

    fun reloadData(gitlabAccount: GitlabAccount? = null) { // TODO: Implement feature to reload data only for a provided account
        filtered = false
        progressIndicator.run(object : Task.Backgroundable(project, "Repositories download", true, ALWAYS_BACKGROUND) {

            override fun run(indicator: ProgressIndicator) {
                val gitlabAccounts = loadAccounts()
                val localGitlabProjectsMap = gitlabApiManager.retrieveGitlabProjectsFor(gitlabAccounts)
                gitlabProjectsMap = localGitlabProjectsMap
                updatePagingPointers(localGitlabProjectsMap.values)

                ui.repositoryModel.availableAccounts = gitlabAccounts
                applicationManager.invokeLater { updateAccountProjects() }
            }
        })
    }

    private fun doGlobalSearch(globalSearchText: String?) {
        filtered = globalSearchText != null && globalSearchText.isNotBlank()
        progressIndicator.run(object : Task.Backgroundable(project, "Filtered repositories download", true, ALWAYS_BACKGROUND) {

            override fun run(indicator: ProgressIndicator) {
                val gitlabAccounts: Collection<GitlabAccount> = gitlabProjectsMap?.keys ?: loadAccounts()
                val localGitlabProjectsMap = gitlabApiManager.retrieveGitlabProjectsFor(gitlabAccounts, globalSearchText)
                gitlabProjectsMap = localGitlabProjectsMap
                updatePagingPointers(localGitlabProjectsMap.values)

                ui.repositoryModel.availableAccounts = gitlabAccounts
                applicationManager.invokeLater { updateAccountProjects() }
            }
        })
    }

    private fun loadAccounts(): List<GitlabAccount> {
        val gitlabAccounts: List<GitlabAccount> = gitlabSettings.getAllGitlabAccountsBy { authenticationManager.hasAuthenticationTokenFor(it) }
        val gitlabUsersMap = gitlabApiManager.retrieveGitlabUsersFor(gitlabAccounts)

        applicationManager.invokeLater {
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
                    it.values.forEach { pagerProxy -> pagerProxy.loadPreviousPage() }
                    updatePagingPointers(it.values)
                    applicationManager.invokeLater { updateAccountProjects() }
                }
            }
        })
    }

    private fun loadNextRepositories() {
        progressIndicator.run(object : Task.Backgroundable(project, "Next repositories download", true, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                gitlabProjectsMap?.let {
                    it.values.forEach { pagerProxy -> pagerProxy.loadNextPage() }
                    updatePagingPointers(it.values)
                    applicationManager.invokeLater { updateAccountProjects() }
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
