package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.api.PagerProxy
import com.github.xeonkryptos.integration.gitlab.api.gitlab.GitlabGroupsApi
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabGroup
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.ui.general.CollectionListModelExt
import com.github.xeonkryptos.integration.gitlab.ui.general.CustomListToolbarDecorator
import com.github.xeonkryptos.integration.gitlab.ui.general.NextActionButton
import com.github.xeonkryptos.integration.gitlab.ui.general.PreviousActionButton
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionListModel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.progress.ProgressVisibilityManager
import com.intellij.util.ui.cloneDialog.ListWithSearchComponent
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent

class GroupChooserDialog(private val project: Project, private val gitlabAccount: GitlabAccount) : DialogWrapper(project, true, IdeModalityType.IDE) {

    companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val groupsModel: CollectionListModel<GitlabGroup> = CollectionListModelExt()
    private val listWithSearchComponent = ListWithSearchComponent(groupsModel, object : SimpleListCellRenderer<GitlabGroup>() {
        override fun customize(list: JList<out GitlabGroup>, value: GitlabGroup?, index: Int, selected: Boolean, hasFocus: Boolean) {
            text = value?.fullName
        }
    }).apply {
        searchField.addKeyboardListener(object : KeyStrokeAdapter() {
            override fun keyTyped(event: KeyEvent?) {
                if (event?.isControlDown == true && event.keyChar == KeyEvent.VK_ENTER.toChar()) {
                    reload(searchField.text)
                }
            }
        })
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (searchField.text == null || searchField.text.isBlank()) reload(null)
            }
        })
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.selectionModel.addListSelectionListener { this@GroupChooserDialog.isOKActionEnabled = list.selectedValue != null }
    }

    private val progressIndicator = object : ProgressVisibilityManager() {

        override fun getModalityState(): ModalityState = ModalityState.any()

        override fun setProgressVisible(visible: Boolean) {
            listWithSearchComponent.list.setPaintBusy(visible)
        }
    }
    private val gitlabGroupsApi = service<GitlabGroupsApi>()
    private val resetAction: Action = ResetAction()

    @Volatile
    private var groupsPagerProxy: PagerProxy<List<GitlabGroup>>? = null

    val selectedGroup: GitlabGroup?
        get() = listWithSearchComponent.list.selectedValue

    init {
        title = GitlabBundle.message("share.group.chooser.dialog.title")
        isAutoAdjustable = true
        isOKActionEnabled = false
        init()
        centerRelativeToParent()
        setOKButtonText(GitlabBundle.message("share.group.chooser.button"))
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction, resetAction, cancelAction)
    }

    override fun createCenterPanel(): JComponent = CustomListToolbarDecorator(listWithSearchComponent.list).initPosition()
        .disableUpAction()
        .disableDownAction()
        .disableAddAction()
        .disableRemoveAction()
        .addExtraAction(PreviousActionButton(GitlabBundle.message("share.group.chooser.dialog.previous.action"),
                                             { groupsPagerProxy?.hasPreviousPage() ?: false },
                                             { loadAnotherPage { groupsPagerProxy?.loadPreviousPage() } }))
        .addExtraAction(NextActionButton(GitlabBundle.message("share.group.chooser.dialog.next.action"),
                                         { groupsPagerProxy?.hasNextPage() ?: false },
                                         { loadAnotherPage { groupsPagerProxy?.loadNextPage() } }))
        .createPanel()

    override fun show() {
        reload()
        super.show()
    }

    private fun reload(searchText: String? = null) {
        val backgroundTask = object : Task.Backgroundable(project, GitlabBundle.message("share.group.chooser.dialog.load"), true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    groupsPagerProxy = gitlabGroupsApi.loadAvailableGroupsFor(gitlabAccount, searchText)
                    val loadedGroups = groupsPagerProxy?.currentData
                    SwingUtilities.invokeLater {
                        groupsModel.removeAll()
                        if (loadedGroups != null) groupsModel.add(loadedGroups)
                    }
                } catch (e: Exception) {
                    LOG.warn(e)
                    setErrorText(GitlabBundle.message("share.group.chooser.dialog.loading.failed", e.toString()))
                }
            }
        }
        progressIndicator.run(backgroundTask)
    }

    private fun loadAnotherPage(loaderFunction: () -> List<GitlabGroup>?) {
        val backgroundTask = object : Task.Backgroundable(project, GitlabBundle.message("share.group.chooser.dialog.load"), true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val loadedGroups = loaderFunction()
                    SwingUtilities.invokeLater {
                        groupsModel.removeAll()
                        if (loadedGroups != null) groupsModel.add(loadedGroups)
                    }
                } catch (e: Exception) {
                    LOG.warn(e)
                    setErrorText(GitlabBundle.message("share.group.chooser.dialog.loading.failed", e.toString()))
                }
            }
        }
        progressIndicator.run(backgroundTask)
    }

    private inner class ResetAction : DialogWrapperAction(GitlabBundle.message("share.group.chooser.reset.button")) {

        override fun doAction(e: ActionEvent?) {
            listWithSearchComponent.list.selectionModel.clearSelection()
            close(OK_EXIT_CODE)
        }
    }
}