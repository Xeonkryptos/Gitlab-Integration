package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabProjectApi
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
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
        @JvmStatic
        private val LOG = GitlabUtil.LOG
    }

    private val wrapper: Wrapper = Wrapper().apply { border = JBEmptyBorder(UIUtil.PANEL_REGULAR_INSETS) }

    private val cloneRepositoryUI: CloneRepositoryUI = CloneRepositoryUI(project).apply {
        addClonePathListener { newClonePath ->
            clonePath = newClonePath
            dialogStateListener.onOkActionEnabled(clonePath != null)
        }
    }

    private val gitlabProjectApi: GitlabProjectApi = GitlabProjectApi(project)
    private val gitlabDataService = GitlabDataService.getInstance(project)

    private var clonePath: String? = null

    init {
        val tokenLoginUI = TokenLoginUI(project) {
            val projectList = gitlabProjectApi.retrieveProjectList()
            cloneRepositoryUI.updateProjectList(projectList)

            wrapper.setContent(cloneRepositoryUI.repositoryPanel)
        }

        val gitlabHosts = gitlabDataService.gitlabData?.gitlabHosts
        if (gitlabHosts == null && gitlabHosts?.isEmpty() == true) {
            wrapper.setContent(tokenLoginUI.tokenLoginPanel)
        } else {
            wrapper.setContent(cloneRepositoryUI.repositoryPanel)
        }
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val parent = Paths.get(cloneRepositoryUI.directoryField.text).toAbsolutePath().parent
        val destinationValidation = CloneDvcsValidationUtils.createDestination("") // TODO
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
