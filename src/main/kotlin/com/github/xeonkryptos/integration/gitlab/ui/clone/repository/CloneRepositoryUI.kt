package com.github.xeonkryptos.integration.gitlab.ui.clone.repository

import com.github.xeonkryptos.integration.gitlab.ui.clone.repository.event.ClonePathEvent
import com.github.xeonkryptos.integration.gitlab.ui.clone.repository.event.ClonePathEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.CustomListToolbarDecorator
import com.github.xeonkryptos.integration.gitlab.ui.general.NextActionButton
import com.github.xeonkryptos.integration.gitlab.ui.general.PreviousActionButton
import com.github.xeonkryptos.integration.gitlab.ui.general.event.GlobalSearchTextEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.GlobalSearchTextEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.PagingEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.PagingEventListener
import com.github.xeonkryptos.integration.gitlab.ui.general.event.ReloadDataEvent
import com.github.xeonkryptos.integration.gitlab.ui.general.event.ReloadDataEventListener
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.util.WaitUntilInputFinishedThrottler
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI
import git4idea.remote.GitRememberedInputs
import java.awt.FlowLayout
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.EventListenerList

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class CloneRepositoryUI(private val project: Project) : Disposable {

    private val eventListeners: EventListenerList = EventListenerList()

    private val globalSearchWaitUntilInputFinishedThrottler: WaitUntilInputFinishedThrottler = WaitUntilInputFinishedThrottler {
        SwingUtilities.invokeLater {
            val globalSearchTextEvent = GlobalSearchTextEvent(this, searchField.text)
            if (searchField.text.isBlank()) {
                fireGlobalSearchTextDeleted(globalSearchTextEvent)
            } else {
                fireGlobalSearchTextChanged(globalSearchTextEvent)
            }
        }
    }

    internal val repositoryModel: CloneRepositoryUIModel = CloneRepositoryUIModel()
    internal val usersPanel: JPanel = JPanel(FlowLayout(FlowLayout.LEADING, JBUI.scale(1), 0))
    internal val gitlabProjectItemsList: JBList<GitlabProjectListItem> = JBList(repositoryModel).apply {
        cellRenderer = GitlabProjectListCellRenderer { repositoryModel.availableAccounts }
        selectionModel.addListSelectionListener {
            val selectedProject = selectedValue?.gitlabProject
            val clonePathEvent = ClonePathEvent(this, selectedProject)
            fireClonePathChangedEvent(clonePathEvent)
        }
    }
    private val searchField: SearchTextField = SearchTextField(false).apply {
        addKeyboardListener(object : KeyStrokeAdapter() {
            override fun keyTyped(event: KeyEvent?) {
                globalSearchWaitUntilInputFinishedThrottler.onNewInputReceived()
            }
        })
    }
    private val repositoryListPanel: JPanel by lazy {
        CustomListToolbarDecorator(gitlabProjectItemsList).initPosition()
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

    private var controller: CloneRepositoryUIControl = CloneRepositoryUIControl(project, this)

    init {
        repositoryPanel = panel {
            row {
                cell(searchField).horizontalAlign(HorizontalAlign.FILL).resizableColumn()
                cell(usersPanel).horizontalAlign(HorizontalAlign.RIGHT)
            }
            row { cell(repositoryListPanel).horizontalAlign(HorizontalAlign.FILL).verticalAlign(VerticalAlign.FILL).resizableColumn() }.resizableRow()
            row(GitlabBundle.message("clone.dialog.directory.field")) { cell(directoryField).horizontalAlign(HorizontalAlign.FILL).resizableColumn() }
        }
        controller.registerBehaviourListeners()

        Disposer.register(this, globalSearchWaitUntilInputFinishedThrottler)
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

    private fun fireGlobalSearchTextChanged(event: GlobalSearchTextEvent) =
            eventListeners.getListeners(GlobalSearchTextEventListener::class.java).forEach { listener -> listener.onGlobalSearchTextChanged(event) }

    private fun fireGlobalSearchTextDeleted(event: GlobalSearchTextEvent) =
            eventListeners.getListeners(GlobalSearchTextEventListener::class.java).forEach { listener -> listener.onGlobalSearchTextDeleted(event) }

    fun addGlobalSearchTextEventListener(listener: GlobalSearchTextEventListener) = eventListeners.add(GlobalSearchTextEventListener::class.java, listener)

    @Suppress("unused")
    fun removeGlobalSearchTextEventListener(listener: GlobalSearchTextEventListener) = eventListeners.remove(GlobalSearchTextEventListener::class.java, listener)
}
