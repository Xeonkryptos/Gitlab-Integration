package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.api.UserProvider
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.AnActionButton
import com.intellij.ui.CommonActionsPanel
import com.intellij.ui.CommonActionsPanel.Buttons
import com.intellij.ui.components.JBList
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.cloneDialog.ListWithSearchComponent
import git4idea.remote.GitRememberedInputs
import java.awt.FlowLayout
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Consumer
import javax.swing.JPanel
import javax.swing.JSeparator

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class CloneRepositoryUI(private val project: Project, userProvider: UserProvider) : Disposable {

    private val clonePathListeners = CopyOnWriteArraySet<Consumer<GitlabProject?>>()

    internal val repositoryListModel: CollectionListModelExt<GitlabProjectListItem> = CollectionListModelExt()
    internal val repositoryList: JBList<GitlabProjectListItem>
    internal val usersPanel: JPanel = JPanel(FlowLayout(FlowLayout.LEADING, JBUI.scale(1), 0))
    internal val repositoryListPanel: JPanel

    val directoryField = SelectChildTextFieldWithBrowseButton(ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())).apply {
        val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fcd.isShowFileSystemRoots = true
        fcd.isHideIgnored = false
        addBrowseFolderListener(DvcsBundle.message("clone.destination.directory.browser.title"), DvcsBundle.message("clone.destination.directory.browser.description"), project, fcd)
    }

    val repositoryPanel: JPanel
    var controller: CloneRepositoryUIControl = DefaultCloneRepositoryUIControl(project, this, userProvider)

    init {
        val listWithSearchComponent = ListWithSearchComponent(repositoryListModel, GitlabProjectListCellRenderer { controller.getAvailableAccounts() })
        repositoryList = listWithSearchComponent.list
        repositoryListPanel = CustomListToolbarDecorator(repositoryList, null).initPosition()
            .disableUpAction()
            .disableDownAction()
            .disableAddAction()
            .disableRemoveAction()
            .addExtraAction(PreviousActionButton())
            .addExtraAction(NextActionButton())
            .createPanel()

        repositoryPanel = panel(LCFlags.fill) {
            row {
                cell(isFullWidth = true) {
                    listWithSearchComponent.searchField(growX, pushX)
                    JSeparator(JSeparator.VERTICAL)(growY).withLargeLeftGap()
                    usersPanel().withLargeLeftGap()
                }
            }
            row { repositoryListPanel(grow, push) }
            row(GitlabBundle.message("clone.dialog.directory.field")) { directoryField(growX, pushX) }
        }

        controller.ui = this
    }

    fun reloadData() = controller.reloadData()

    override fun dispose() {}

    fun addClonePathListener(clonePathListener: Consumer<GitlabProject?>) = clonePathListeners.add(clonePathListener)

    interface ExtraActionButton {
        fun verifyUpdateState()
    }

    private inner class PreviousActionButton : AnActionButton(GitlabBundle.message("clone.page.previous"), IconUtil.getMoveUpIcon()), ExtraActionButton {

        init {
            addCustomUpdater { controller.hasPreviousRepositories() }
        }

        override fun getShortcut(): ShortcutSet {
            return CommonActionsPanel.getCommonShortcut(Buttons.UP)
        }

        override fun actionPerformed(e: AnActionEvent) {
            controller.loadPreviousRepositories()
        }

        override fun verifyUpdateState() {
            isEnabled = controller.hasPreviousRepositories()
        }
    }

    private inner class NextActionButton : AnActionButton(GitlabBundle.message("clone.page.next"), IconUtil.getMoveDownIcon()), ExtraActionButton {

        init {
            addCustomUpdater { controller.hasNextRepositories() }
        }

        override fun getShortcut(): ShortcutSet {
            return CommonActionsPanel.getCommonShortcut(Buttons.DOWN)
        }

        override fun actionPerformed(e: AnActionEvent) {
            controller.loadNextRepositories()
        }

        override fun verifyUpdateState() {
            isEnabled = controller.hasNextRepositories()
        }
    }
}
