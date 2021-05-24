package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ClonePathEvent
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event.ClonePathEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.GlobalSearchTextEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.GlobalSearchTextEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.PagingEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.PagingEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.ReloadDataEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.ReloadDataEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.CustomListToolbarDecorator
import com.github.xeonkryptos.integration.gitlab.ui.general.NextActionButton
import com.github.xeonkryptos.integration.gitlab.ui.general.PreviousActionButton
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
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
class CloneRepositoryUI(private val project: Project) : Disposable {

    private val eventListeners: EventListenerList = EventListenerList()

    internal val repositoryModel: CloneRepositoryUIModel = DefaultCloneRepositoryUIModel()
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
        CustomListToolbarDecorator(listWithSearchComponent.list).initPosition()
            .disableUpAction()
            .disableDownAction()
            .disableAddAction()
            .disableRemoveAction()
            .addExtraAction(PreviousActionButton(GitlabBundle.message("clone.page.previous"), { repositoryModel.hasPreviousRepositories }, { fireOnPreviousPagingEvent(PagingEvent(it)) }))
            .addExtraAction(NextActionButton(GitlabBundle.message("clone.page.next"), { repositoryModel.hasNextRepositories }, { fireOnNextPagingEvent(PagingEvent(it)) }))
            .createPanel()
    }

    val directoryField = SelectChildTextFieldWithBrowseButton(ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())).apply {
        val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fcd.isShowFileSystemRoots = true
        fcd.isHideIgnored = false
        addBrowseFolderListener(DvcsBundle.message("clone.destination.directory.browser.title"), DvcsBundle.message("clone.destination.directory.browser.description"), project, fcd)
    }

    val repositoryPanel: JPanel
    var controller: CloneRepositoryUIControl = DefaultCloneRepositoryUIControl(project, this)

    init {
        repositoryPanel = panel(LCFlags.fill) {
            row {
                cell(isFullWidth = true) {
                    listWithSearchComponent.searchField(growX, pushX)
                    button(GitlabBundle.message("button.globalSearch")) {
                        val searchText: String? = listWithSearchComponent.searchField.text
                        if (searchText?.isNotBlank() == true) {
                            fireGlobalSearchTextChanged(GlobalSearchTextEvent(this, searchText))
                        }
                    }.enableIf(object : ComponentPredicate() {
                        override fun addListener(listener: (Boolean) -> Unit) {
                            listWithSearchComponent.searchField.addDocumentListener(object : DocumentAdapter() {
                                override fun textChanged(e: DocumentEvent) {
                                    listener(invoke())

                                    val searchText: String? = listWithSearchComponent.searchField.text
                                    if (searchText?.isNotBlank() != true) {
                                        fireGlobalSearchTextDeleted(GlobalSearchTextEvent(this, searchText))
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

    @Suppress("unused")
    fun removeReloadDataEventListener(listener: ReloadDataEventListener) = eventListeners.remove(ReloadDataEventListener::class.java, listener)

    @Suppress("MemberVisibilityCanBePrivate")
    fun fireClonePathChangedEvent(event: ClonePathEvent) = eventListeners.getListeners(ClonePathEventListener::class.java).forEach { listener -> listener.onClonePathChanged(event) }

    fun addClonePathEventListener(listener: ClonePathEventListener) = eventListeners.add(ClonePathEventListener::class.java, listener)

    @Suppress("unused")
    fun removeClonePathEventListener(listener: ClonePathEventListener) = eventListeners.remove(ClonePathEventListener::class.java, listener)

    @Suppress("MemberVisibilityCanBePrivate")
    fun fireOnPreviousPagingEvent(event: PagingEvent) = eventListeners.getListeners(PagingEventListener::class.java).forEach { listener -> listener.onPreviousPage(event) }

    @Suppress("MemberVisibilityCanBePrivate")
    fun fireOnNextPagingEvent(event: PagingEvent) = eventListeners.getListeners(PagingEventListener::class.java).forEach { listener -> listener.onNextPage(event) }

    fun addPagingEventListener(listener: PagingEventListener) = eventListeners.add(PagingEventListener::class.java, listener)

    @Suppress("unused")
    fun removePagingEventListener(listener: PagingEventListener) = eventListeners.remove(PagingEventListener::class.java, listener)

    fun fireGlobalSearchTextChanged(event: GlobalSearchTextEvent) =
        eventListeners.getListeners(GlobalSearchTextEventListener::class.java).forEach { listener -> listener.onGlobalSearchTextChanged(event) }

    fun fireGlobalSearchTextDeleted(event: GlobalSearchTextEvent) =
        eventListeners.getListeners(GlobalSearchTextEventListener::class.java).forEach { listener -> listener.onGlobalSearchTextDeleted(event) }

    fun addGlobalSearchTextEventListener(listener: GlobalSearchTextEventListener) = eventListeners.add(GlobalSearchTextEventListener::class.java, listener)

    @Suppress("unused")
    fun removeGlobalSearchTextEventListener(listener: GlobalSearchTextEventListener) = eventListeners.remove(GlobalSearchTextEventListener::class.java, listener)
}
