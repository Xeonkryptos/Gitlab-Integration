package com.github.xeonkryptos.integration.gitlab.ui.clone

import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabAccountStateNotifier
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.ui.clone.repository.CloneRepositoryUI
import com.github.xeonkryptos.integration.gitlab.ui.clone.repository.event.ClonePathEvent
import com.github.xeonkryptos.integration.gitlab.ui.clone.repository.event.ClonePathEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.ReloadDataEvent
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotificationIdsHolder
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import git4idea.checkout.GitCheckoutProvider
import git4idea.commands.Git
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * @author Xeonkryptos
 * @since 06.09.2020
 */
class GitlabCloneDialogExtensionComponent(private val project: Project) : VcsCloneDialogExtensionComponent() {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val gitlabSettings = service<GitlabSettingsService>().state
    private val authenticationManager = service<AuthenticationManager>()

    private val wrapper: Wrapper = Wrapper().apply { border = JBEmptyBorder(UIUtil.PANEL_REGULAR_INSETS) }

    private val cloneRepositoryUI: CloneRepositoryUI = CloneRepositoryUI(project).apply {
        addClonePathEventListener(object : ClonePathEventListener {
            override fun onClonePathChanged(event: ClonePathEvent) {
                gitlabProject = event.gitlabProject
                dialogStateListener.onOkActionEnabled(gitlabProject != null)
            }
        })
    }
    private val gitlabLoginPanel = EmbeddedGitlabLoginPanel(project).apply {
        addGitlabLoginActionListener(object : EmbeddedGitlabLoginPanel.GitlabLoginActionListener {
            override fun onSuccessfulLogin(event: EmbeddedGitlabLoginPanel.GitlabLoginEvent) { switchToRepoScenery() }
        })
    }

    private var gitlabProject: GitlabProject? = null

    init {
        initMessaging()

        Disposer.register(this, cloneRepositoryUI)
        Disposer.register(this, gitlabLoginPanel)

        val hasSignedInAccounts = gitlabSettings.hasGitlabAccountBy { authenticationManager.hasAuthenticationTokenFor(it) }
        if (!hasSignedInAccounts) {
            wrapper.setContent(gitlabLoginPanel)
        } else {
            cloneRepositoryUI.fireReloadDataEvent(ReloadDataEvent(this))
            wrapper.setContent(cloneRepositoryUI.repositoryPanel)

            dialogStateListener.onOkActionEnabled(false)
            dialogStateListener.onOkActionNameChanged(GitlabBundle.message("clone.button"))
        }
    }

    private fun initMessaging() {
        val connection = ApplicationManager.getApplication().messageBus.connect(this)
        connection.subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, object : GitlabLoginChangeNotifier {
            override fun onSignIn(gitlabAccount: GitlabAccount) {
                gitlabSettings.registerGitlabAccount(gitlabAccount)
                SwingUtilities.invokeLater { switchToRepoScenery() }
            }

            override fun onSignOut(gitlabAccount: GitlabAccount) {
                SwingUtilities.invokeLater { switchToLoginScenery() }
            }
        })
        connection.subscribe(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC, object : GitlabAccountStateNotifier {
            override fun onGitlabAccountDeleted(gitlabAccount: GitlabAccount) {
                SwingUtilities.invokeLater { switchToLoginScenery() }
            }
        })
    }

    @RequiresEdt
    private fun switchToLoginScenery() {
        val hasLoggedInAccount = gitlabSettings.hasGitlabAccountBy { authenticationManager.hasAuthenticationTokenFor(it) }
        if (!hasLoggedInAccount) {
            wrapper.setContent(gitlabLoginPanel)

            dialogStateListener.onOkActionEnabled(true)
            dialogStateListener.onOkActionNameChanged(GitlabBundle.message("accounts.log.in"))
        }
    }

    @RequiresEdt
    private fun switchToRepoScenery() {
        val hasLoggedInAccount = gitlabSettings.hasGitlabAccountBy { authenticationManager.hasAuthenticationTokenFor(it) }
        if (hasLoggedInAccount) {
            wrapper.setContent(cloneRepositoryUI.repositoryPanel)

            dialogStateListener.onOkActionEnabled(gitlabProject != null)
            dialogStateListener.onOkActionNameChanged(GitlabBundle.message("clone.button"))
        }
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val localGitlabProject = gitlabProject
        if (localGitlabProject != null) {
            val parent = Paths.get(cloneRepositoryUI.directoryField.text).toAbsolutePath().parent
            val destinationValidation = CloneDvcsValidationUtils.createDestination(cloneRepositoryUI.directoryField.text)
            if (destinationValidation == null) {
                val lfs = LocalFileSystem.getInstance()
                var destinationParent = lfs.findFileByIoFile(parent.toFile())
                if (destinationParent == null) {
                    destinationParent = lfs.refreshAndFindFileByIoFile(parent.toFile())
                }
                if (destinationParent == null) {
                    LOG.error("Clone Failed. Destination doesn't exist")
                    GitlabNotifications.showError(project,
                                                  GitlabNotificationIdsHolder.CLONE_UNABLE_TO_FIND_DESTINATION,
                                                  GitlabBundle.message("clone.dialog.clone.failed"),
                                                  GitlabBundle.message("clone.error.unable.to.find.dest"))
                } else {
                    val directoryName = Paths.get(cloneRepositoryUI.directoryField.text).fileName.toString()
                    val parentDirectory = parent.toAbsolutePath().toString()

                    val cloneUrl = if (localGitlabProject.gitlabAccount.useSSH) localGitlabProject.sshUrlToRepo else localGitlabProject.httpUrlToRepo
                    GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, destinationParent, cloneUrl, directoryName, parentDirectory)
                }
            } else {
                LOG.error("Unable to create destination directory", destinationValidation.message)
                GitlabNotifications.showError(project,
                                              GitlabNotificationIdsHolder.CLONE_UNABLE_TO_CREATE_DESTINATION_DIR,
                                              GitlabBundle.message("clone.dialog.clone.failed"),
                                              GitlabBundle.message("clone.error.unable.to.create.dest.dir"))
            }
        } else {
            LOG.error("Unable to construct clone destination. Missing host url and/or clone path")
        }
    }

    override fun doValidateAll(): List<ValidationInfo> {
        val list = ArrayList<ValidationInfo>()
        if (isInLoginScenery()) {
            list.addAll(gitlabLoginPanel.currentValidationInfo)
        } else {
            ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkDirectory(cloneRepositoryUI.directoryField.text, cloneRepositoryUI.directoryField.textField))
        }
        return list
    }

    private fun isInLoginScenery(): Boolean = wrapper.targetComponent === gitlabLoginPanel

    override fun getView(): JComponent = wrapper

    override fun onComponentSelected() {
        dialogStateListener.onOkActionNameChanged(GitlabBundle.message("clone.button"))

        val focusManager = IdeFocusManager.getInstance(project)
        getPreferredFocusedComponent()?.let { focusManager.requestFocus(it, true) }
    }
}
