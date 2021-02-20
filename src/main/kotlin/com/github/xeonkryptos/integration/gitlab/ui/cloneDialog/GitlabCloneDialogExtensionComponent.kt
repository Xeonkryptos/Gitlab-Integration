package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.api.GitlabUserProvider
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabAccountStateNotifier
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.github.xeonkryptos.integration.gitlab.util.invokeOnDispatchThread
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.openapi.application.ApplicationManager
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

/**
 * @author Xeonkryptos
 * @since 06.09.2020
 */
class GitlabCloneDialogExtensionComponent(private val project: Project) : VcsCloneDialogExtensionComponent() {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val gitlabApiManager = GitlabApiManager(project)
    private val gitlabSettings = GitlabSettingsService.getInstance(project).state
    private val authenticationManager = AuthenticationManager.getInstance(project)

    private val wrapper: Wrapper = object : Wrapper() {
        override fun setContent(wrapped: JComponent?) {
            super.setContent(wrapped)

            revalidate()
            repaint()
        }
    }.apply { border = JBEmptyBorder(UIUtil.PANEL_REGULAR_INSETS) }

    private val cloneRepositoryUI: CloneRepositoryUI by lazy {
        CloneRepositoryUI(project, GitlabUserProvider(gitlabApiManager, gitlabSettings)).apply {
            addClonePathListener { newGitlabProject ->
                gitlabProject = newGitlabProject
                controller.updateProjectName(cloneProjectName)
                dialogStateListener.onOkActionEnabled(gitlabProject != null)
            }
        }
    }

    private var gitlabProject: GitlabProject? = null
        set(value) {
            cloneProjectName = value?.viewableProjectPath?.substringAfterLast('/')
            field = value
        }
    private var cloneProjectName: String? = null

    private val tokenLoginUI = TokenLoginUI(project, gitlabApiManager)

    init {
        initMessaging()

        Disposer.register(this, cloneRepositoryUI)

        val hasSignedInAccounts = gitlabSettings.hasGitlabAccountBy { authenticationManager.hasAuthenticationTokenFor(it) }
        if (!hasSignedInAccounts) {
            wrapper.setContent(tokenLoginUI.tokenLoginPanel)
        } else {
            cloneRepositoryUI.reloadData()
            wrapper.setContent(cloneRepositoryUI.repositoryPanel)
        }
    }

    private fun initMessaging() {
        val connection = ApplicationManager.getApplication().messageBus.connect(this)
        connection.subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, object : GitlabLoginChangeNotifier {
            override fun onSignIn(gitlabAccount: GitlabAccount) {
                ApplicationManager.getApplication().invokeOnDispatchThread(wrapper) { switchToRepoScenery() }
            }

            override fun onSignOut(gitlabAccount: GitlabAccount) {
                ApplicationManager.getApplication().invokeOnDispatchThread(wrapper) { switchToLoginScenery() }
            }
        })
        connection.subscribe(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC, object : GitlabAccountStateNotifier {
            override fun onGitlabAccountDeleted(gitlabAccount: GitlabAccount) {
                ApplicationManager.getApplication().invokeOnDispatchThread(wrapper) { switchToLoginScenery() }
            }
        })
    }

    @RequiresEdt
    private fun switchToLoginScenery() {
        val hasLoggedInAccount = gitlabSettings.hasGitlabAccountBy { authenticationManager.hasAuthenticationTokenFor(it) }
        if (!hasLoggedInAccount) {
            wrapper.setContent(tokenLoginUI.tokenLoginPanel)
        }
    }

    @RequiresEdt
    private fun switchToRepoScenery() {
        val hasLoggedInAccount = gitlabSettings.hasGitlabAccountBy { authenticationManager.hasAuthenticationTokenFor(it) }
        if (hasLoggedInAccount) {
            wrapper.setContent(cloneRepositoryUI.repositoryPanel)
        }
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val localGitlabProject = gitlabProject
        if (localGitlabProject?.httpProjectUrl != null) {
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
                    GitlabNotifications.showError(project, GitlabBundle.message("clone.dialog.clone.failed"), GitlabBundle.message("clone.error.unable.to.find.dest"))
                } else {
                    val directoryName = Paths.get(cloneRepositoryUI.directoryField.text).fileName.toString()
                    val parentDirectory = parent.toAbsolutePath().toString()

                    GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, destinationParent, localGitlabProject.httpProjectUrl, directoryName, parentDirectory)
                }
            } else {
                LOG.error("Unable to create destination directory", destinationValidation.message)
                GitlabNotifications.showError(project, GitlabBundle.message("clone.dialog.clone.failed"), GitlabBundle.message("clone.error.unable.to.create.dest.dir"))
            }
        } else {
            LOG.error("Unable to construct clone destination. Missing host url and/or clone path")
        }
    }

    override fun doValidateAll(): List<ValidationInfo> {
        val list = ArrayList<ValidationInfo>()
        ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkDirectory(cloneRepositoryUI.directoryField.text, cloneRepositoryUI.directoryField.textField))
        return list
    }

    override fun getView(): JComponent = wrapper

    override fun onComponentSelected() {
        dialogStateListener.onOkActionNameChanged(GitlabBundle.message("clone.button"))

        val focusManager = IdeFocusManager.getInstance(project)
        getPreferredFocusedComponent()?.let { focusManager.requestFocus(it, true) }
    }
}
