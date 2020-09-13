package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.ui.component.TreeWithSearchComponent
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import git4idea.checkout.GitCheckoutProvider
import git4idea.commands.Git
import git4idea.remote.GitRememberedInputs
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * @author Xeonkryptos
 * @since 06.09.2020
 */
class GitlabCloneDialogExtensionComponent(private val project: Project) : VcsCloneDialogExtensionComponent() {

    private companion object {
        @JvmStatic
        private val LOG = GitlabUtil.LOG
    }

    private val wrapper: Wrapper = Wrapper()
    private val searchField: SearchTextField
    private val tree: Tree
    private val directoryField = SelectChildTextFieldWithBrowseButton(ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())).apply {
        val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fcd.isShowFileSystemRoots = true
        fcd.isHideIgnored = false
        addBrowseFolderListener(DvcsBundle.getString("clone.destination.directory.browser.title"), DvcsBundle.getString("clone.destination.directory.browser.description"), project, fcd)
    }

    init {
        val treeRoot = DefaultMutableTreeNode()

        // TODO: Dummy test data. Remove later
        val xeonkryptosNode = DefaultMutableTreeNode("Xeonkryptos")
        xeonkryptosNode.add(DefaultMutableTreeNode("eclipse-project-creator"))
        treeRoot.add(xeonkryptosNode)
        treeRoot.add(DefaultMutableTreeNode("funartic"))

        val treeModel = DefaultTreeModel(treeRoot)

        val treeWithSearchComponent = TreeWithSearchComponent(treeModel)
        tree = treeWithSearchComponent.tree
        searchField = treeWithSearchComponent.searchField

        val repositoryPanel = panel(LCFlags.fill) {
            row { searchField(growX, pushX) }
            row { ScrollPaneFactory.createScrollPane(tree)(grow, push) }
            row(GitlabBundle.message("clone.dialog.directory.field")) { directoryField(growX, pushX) }
        }
        repositoryPanel.border = JBEmptyBorder(UIUtil.PANEL_REGULAR_INSETS)

        wrapper.setContent(repositoryPanel)
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val parent = Paths.get(directoryField.text).toAbsolutePath().parent
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
        val directoryName = Paths.get(directoryField.text).fileName.toString()
        val parentDirectory = parent.toAbsolutePath().toString()

        GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, destinationParent, "selectedUrl", directoryName, parentDirectory) // TODO
    }

    override fun doValidateAll(): List<ValidationInfo> {
        val list = ArrayList<ValidationInfo>()
        ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkDirectory(directoryField.text, directoryField.textField))
        return list
    }

    override fun getView(): JComponent = wrapper

    override fun onComponentSelected() {
        dialogStateListener.onOkActionNameChanged(GitlabBundle.message("clone.button"))
        //        updateSelectedUrl() // TODO

        val focusManager = IdeFocusManager.getInstance(project)
        getPreferredFocusedComponent()?.let { focusManager.requestFocus(it, true) }
    }
}
