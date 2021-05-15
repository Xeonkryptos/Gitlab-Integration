package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.api.UserProvider
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ClonePathEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ClonePathEventListener
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.GlobalSearchTextEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.GlobalSearchTextEventListener
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.PagingEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.PagingEventListener
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ReloadDataEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ReloadDataEventListener
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
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.cloneDialog.ListWithSearchComponent
import git4idea.remote.GitRememberedInputs
import java.awt.FlowLayout
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.event.DocumentEvent
import javax.swing.event.EventListenerList

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class CloneRepositoryUI(private val project: Project, userProvider: UserProvider) : Disposable {

    private val eventListeners: EventListenerList = EventListenerList()

    internal val repositoryModel: DefaultCloneRepositoryUIModel = DefaultCloneRepositoryUIModel()
    internal val usersPanel: JPanel = JPanel(FlowLayout(FlowLayout.LEADING, JBUI.scale(1), 0))
    internal val listWithSearchComponent: ListWithSearchComponent<GitlabProjectListItem> =
        ListWithSearchComponent(repositoryModel, GitlabProjectListCellRenderer { repositoryModel.availableAccounts }).apply {
            list.selectionModel.addListSelectionListener {
                val selectedProject = list.selectedValue?.gitlabProject
                val clonePathEvent = ClonePathEvent(this, selectedProject)
                fireClonePathChangedEvent(clonePathEvent)
            }
            searchField.addKeyboardListener(object : KeyStrokeAdapter() {
                override fun keyTyped(event: KeyEvent?) {
                    if (event?.isControlDown == true && event.keyChar == KeyEvent.VK_ENTER.toChar()) {
                        val globalSearchTextEvent = GlobalSearchTextEvent(this, searchField.text)
                        fireGlobalSearchTextChanged(globalSearchTextEvent)
                    }
                }
            })
        }
    private val repositoryListPanel: JPanel by lazy {
        CustomListToolbarDecorator(listWithSearchComponent.list, null).initPosition()
            .disableUpAction()
            .disableDownAction()
            .disableAddAction()
            .disableRemoveAction()
            .addExtraAction(PreviousActionButton())
            .addExtraAction(NextActionButton())
            .createPanel()
    }

    val directoryField = SelectChildTextFieldWithBrowseButton(ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())).apply {
        val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fcd.isShowFileSystemRoots = true
        fcd.isHideIgnored = false
        addBrowseFolderListener(DvcsBundle.message("clone.destination.directory.browser.title"), DvcsBundle.message("clone.destination.directory.browser.description"), project, fcd)
    }

    val repositoryPanel: JPanel
    var controller: CloneRepositoryUIControl = DefaultCloneRepositoryUIControl(project, this, userProvider)

    init {
        repositoryPanel = panel(LCFlags.fill) {
            row {
                cell(isFullWidth = true) {
                    listWithSearchComponent.searchField(growX, pushX)
                    button(GitlabBundle.message("button.globalSearch")) {}.enableIf(object : ComponentPredicate() {
                        override fun addListener(listener: (Boolean) -> Unit) {
                            listWithSearchComponent.searchField.addDocumentListener(object : DocumentAdapter() {
                                override fun textChanged(e: DocumentEvent) {
                                    listener.invoke(invoke())

                                    val searchText: String? = listWithSearchComponent.searchField.text
                                    val globalSearchTextEvent = GlobalSearchTextEvent(this, searchText)
                                    if (searchText?.isNotBlank() == true) {
                                        fireGlobalSearchTextChanged(globalSearchTextEvent)
                                    } else {
                                        fireGlobalSearchTextDeleted(globalSearchTextEvent)
                                    }
                                }
                            })
                        }

                        override fun invoke(): Boolean = listWithSearchComponent.searchField.text?.isNotBlank() ?: false
                    })
                    JSeparator(JSeparator.VERTICAL)(growY).withLargeLeftGap()
                    usersPanel().withLargeLeftGap()
                }
            }
            row { repositoryListPanel(grow, push) }
            row(GitlabBundle.message("clone.dialog.directory.field")) { directoryField(growX, pushX) }
        }
        controller.registerBehaviourListeners()
    }

    override fun dispose() {}

    fun fireReloadDataEvent(event: ReloadDataEvent) = eventListeners.getListeners(ReloadDataEventListener::class.java).forEach { listener -> listener.onReloadRequest(event) }

    fun addReloadDataEventListener(listener: ReloadDataEventListener) = eventListeners.add(ReloadDataEventListener::class.java, listener)

    fun removeReloadDataEventListener(listener: ReloadDataEventListener) = eventListeners.remove(ReloadDataEventListener::class.java, listener)

    fun fireClonePathChangedEvent(event: ClonePathEvent) = eventListeners.getListeners(ClonePathEventListener::class.java).forEach { listener -> listener.onClonePathChanged(event) }

    fun addClonePathEventListener(listener: ClonePathEventListener) = eventListeners.add(ClonePathEventListener::class.java, listener)

    fun removeClonePathEventListener(listener: ClonePathEventListener) = eventListeners.remove(ClonePathEventListener::class.java, listener)

    fun fireOnPreviousPagingEvent(event: PagingEvent) = eventListeners.getListeners(PagingEventListener::class.java).forEach { listener -> listener.onPreviousPage(event) }

    fun fireOnNextPagingEvent(event: PagingEvent) = eventListeners.getListeners(PagingEventListener::class.java).forEach { listener -> listener.onNextPage(event) }

    fun addPagingEventListener(listener: PagingEventListener) = eventListeners.add(PagingEventListener::class.java, listener)

    fun removePagingEventListener(listener: PagingEventListener) = eventListeners.remove(PagingEventListener::class.java, listener)

    fun fireGlobalSearchTextChanged(event: GlobalSearchTextEvent) =
        eventListeners.getListeners(GlobalSearchTextEventListener::class.java).forEach { listener -> listener.onGlobalSearchTextChanged(event) }

    fun fireGlobalSearchTextDeleted(event: GlobalSearchTextEvent) =
        eventListeners.getListeners(GlobalSearchTextEventListener::class.java).forEach { listener -> listener.onGlobalSearchTextDeleted(event) }

    fun addGlobalSearchTextEventListener(listener: GlobalSearchTextEventListener) = eventListeners.add(GlobalSearchTextEventListener::class.java, listener)

    fun removeGlobalSearchTextEventListener(listener: GlobalSearchTextEventListener) = eventListeners.remove(GlobalSearchTextEventListener::class.java, listener)

    interface ExtraActionButton {
        fun verifyUpdateState()
    }

    private inner class PreviousActionButton : AnActionButton(GitlabBundle.message("clone.page.previous"), IconUtil.getMoveUpIcon()), ExtraActionButton {

        init {
            addCustomUpdater { repositoryModel.hasPreviousRepositories }
        }

        override fun getShortcut(): ShortcutSet {
            return CommonActionsPanel.getCommonShortcut(Buttons.UP)
        }

        override fun actionPerformed(e: AnActionEvent) {
            fireOnPreviousPagingEvent(PagingEvent(this))
        }

        override fun verifyUpdateState() {
            isEnabled = repositoryModel.hasPreviousRepositories
        }
    }

    private inner class NextActionButton : AnActionButton(GitlabBundle.message("clone.page.next"), IconUtil.getMoveDownIcon()), ExtraActionButton {

        init {
            addCustomUpdater { repositoryModel.hasNextRepositories }
        }

        override fun getShortcut(): ShortcutSet {
            return CommonActionsPanel.getCommonShortcut(Buttons.DOWN)
        }

        override fun actionPerformed(e: AnActionEvent) {
            fireOnNextPagingEvent(PagingEvent(this))
        }

        override fun verifyUpdateState() {
            isEnabled = repositoryModel.hasNextRepositories
        }
    }
}
