package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.panels.Wrapper
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

    private val wrapper: Wrapper = Wrapper().apply { border = JBEmptyBorder(UIUtil.PANEL_REGULAR_INSETS) }

    private val cloneRepositoryUI: CloneRepositoryUI = CloneRepositoryUI(project).apply {
        addClonePathListener { newClonePath ->
            clonePath = newClonePath
            dialogStateListener.onOkActionEnabled(clonePath != null)
        }
    }

    private val gitlabApiManager: GitlabApiManager = GitlabApiManager(project)
    private val gitlabDataService = GitlabDataService.getInstance(project)

    private var clonePath: String? = null

    init {
        val tokenLoginUI = TokenLoginUI(project) {
            wrapper.setContent(cloneRepositoryUI.repositoryPanel)

            loadDataFromGitlab()
        }

        val gitlabHost = gitlabDataService.state?.activeGitlabHost
        if (gitlabHost == null) {
            wrapper.setContent(tokenLoginUI.tokenLoginPanel)
        } else {
            wrapper.setContent(cloneRepositoryUI.repositoryPanel)

            loadDataFromGitlab()
        }
    }

    private fun loadDataFromGitlab() {
        val progressManager = ProgressManager.getInstance()
        // TODO: Generally does for what it's attended for: executing the download in background without blocking the UI but it shows a dialog informing about the download process... Shouldn't be visible
        //  for the user. Additionally, a monitor thread to abort a running HTTP request against the API might be useful, too.
        progressManager.run(object : Task.Backgroundable(project, "Loading data from gitlab repository", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.checkCanceled()
                val avatarImage = gitlabApiManager.getAvatarImage()
                ApplicationManager.getApplication().invokeLater { cloneRepositoryUI.updateUserAvatar(avatarImage) }

                indicator.checkCanceled()
                val gitlabProjects = gitlabApiManager.retrieveProjects()
                ApplicationManager.getApplication().invokeLater { cloneRepositoryUI.updateProjectList(gitlabProjects) }
            }
        })
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val gitlabServerUrl = gitlabApiManager.getGitlabServerUrl()
        val localClonePath = clonePath
        if (gitlabServerUrl == null || localClonePath == null) {
            LOG.error("Unable to construct clone destination. Missing host url and/or clone path")
            return
        }
        val parent = Paths.get(cloneRepositoryUI.directoryField.text).toAbsolutePath().parent
        val destinationValidation = CloneDvcsValidationUtils.createDestination("$gitlabServerUrl/$localClonePath") // TODO
        if (destinationValidation != null) {
            LOG.error("Unable to create destination directory", destinationValidation.message)
            GitlabNotifications.showError(project, GitlabBundle.message("clone.dialog.clone.failed"), GitlabBundle.message("clone.error.unable.to.create.dest.dir"))
            return
        }

        val lfs = LocalFileSystem.getInstance()
        var destinationParent = lfs.findFileByIoFile(parent.toFile())
        if (destinationParent == null) {
            destinationParent = lfs.refreshAndFindFileByIoFile(parent.toFile())
        }
        if (destinationParent == null) {
            LOG.error("Clone Failed. Destination doesn't exist")
            GitlabNotifications.showError(project, GitlabBundle.message("clone.dialog.clone.failed"), GitlabBundle.message("clone.error.unable.to.find.dest"))
            return
        }
        val directoryName = Paths.get(cloneRepositoryUI.directoryField.text).fileName.toString()
        val parentDirectory = parent.toAbsolutePath().toString()

        GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, destinationParent, "selectedUrl", directoryName, parentDirectory) // TODO
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