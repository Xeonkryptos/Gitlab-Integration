package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabVisibility
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.ui.general.AddGitlabAccountEntryDialog
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.util.invokeOnDispatchThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.repo.GitRepositoryManager
import java.nio.file.Path
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class ProjectLinkerDialog(private val project: Project, private val module: Module?) : DialogWrapper(project) {

    companion object {
        private val GITLAB_REPO_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+")
    }

    private val accountComboBoxModel: CollectionComboBoxModel<GitlabAccount> = CollectionComboBoxModel()
    private val visibilityComboBoxModel: CollectionComboBoxModel<GitlabVisibility> = CollectionComboBoxModel<GitlabVisibility>().apply {
        this.add(GitlabVisibility.values().toList())
    }

    private val projectRootManager: ProjectRootManager = ProjectRootManager.getInstance(project)

    private lateinit var rootDirTextField: TextFieldWithBrowseButton
    private lateinit var accountComboBox: ComboBox<GitlabAccount>

    private var projectNamespaceId: Long? = null
    private var selectedAccount: GitlabAccount? = null
    private var rootDirVirtualFile: VirtualFile? = null

    // Properties need to be public because they are accessed via property binding and exactly this binding can only access publicly available properties.
    @Suppress("MemberVisibilityCanBePrivate")
    var projectName: String = ""
    @Suppress("MemberVisibilityCanBePrivate")
    var gitRemote: String = "origin"
    @Suppress("MemberVisibilityCanBePrivate")
    var description: String = ""
    @Suppress("MemberVisibilityCanBePrivate")
    var selectedVisibility: GitlabVisibility = GitlabVisibility.PRIVATE

    @Suppress("MemberVisibilityCanBePrivate")
    var projectNamespace: String = ""
    @Suppress("MemberVisibilityCanBePrivate")
    var rootDir: String = ""

    private val centerPanel: DialogPanel by lazy {
        panel {
            row(label = GitlabBundle.message("share.dialog.account.label")) {
                accountComboBox = comboBox(accountComboBoxModel, object : SimpleListCellRenderer<GitlabAccount>() {
                    override fun customize(list: JList<out GitlabAccount>, value: GitlabAccount?, index: Int, selected: Boolean, hasFocus: Boolean) {
                        text = if (value != null) "${value.getGitlabDomain()}/${value.username}" else ""
                    }
                }).bindItem({ selectedAccount }, { selectedAccount = it }).validationOnApply {
                    if (it.selectedItem == null) {
                        return@validationOnApply error(GitlabBundle.message("share.dialog.missing.account"))
                    }
                    return@validationOnApply null
                }.applyToComponent { addItemListener { projectNamespace = "" } }.component
                link(text = GitlabBundle.message("share.dialog.add.accounts.link")) {
                    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
                    connection.subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, object : GitlabLoginChangeNotifier {
                        override fun onSignIn(gitlabAccount: GitlabAccount) {
                            invokeOnDispatchThread {
                                accountComboBoxModel.add(gitlabAccount)
                                accountComboBox.item = gitlabAccount
                            }
                        }
                    })
                    AddGitlabAccountEntryDialog(project).show()
                    connection.deliverImmediately()
                    connection.disconnect()
                }
            }
            row(label = GitlabBundle.message("share.dialog.project.namespace.label")) {
                val projectNamespaceTextField = textField().bindText(::projectNamespace).apply { enabled(false) }.component
                link(text = GitlabBundle.message("share.dialog.project.namespace.choose.link")) {
                    val groupChooserDialog = GroupChooserDialog(project, accountComboBoxModel.selected!!)
                    if (groupChooserDialog.showAndGet()) {
                        val selectedGroup = groupChooserDialog.selectedGroup
                        projectNamespaceId = selectedGroup?.id
                        projectNamespaceTextField.text = selectedGroup?.fullName
                    }
                }.enabledIf(object : ComponentPredicate() {
                    override fun addListener(listener: (Boolean) -> Unit) {
                        accountComboBoxModel.addListDataListener(object : ListDataListener {
                            override fun intervalAdded(e: ListDataEvent?) {
                                listener(invoke())
                            }

                            override fun intervalRemoved(e: ListDataEvent?) {
                                listener(invoke())
                            }

                            override fun contentsChanged(e: ListDataEvent?) {
                                listener(invoke())
                            }
                        })
                    }

                    override fun invoke(): Boolean = !accountComboBoxModel.isEmpty
                })
            }
            row(label = GitlabBundle.message("share.dialog.project.name.label")) {
                textField().bindText(::projectName).validationOnApply {
                    if (it.text.isBlank()) return@validationOnApply error(GitlabBundle.message("share.validation.no.repo.name"))
                    if (!GITLAB_REPO_PATTERN.matcher(it.text).matches()) return@validationOnApply error(GitlabBundle.message("share.validation.invalid.repo.name"))
                    return@validationOnApply null
                }
                label("").visible(false)
            }
            row(label = GitlabBundle.message("share.dialog.project.visibility.label")) {
                comboBox(visibilityComboBoxModel, renderer = object : SimpleListCellRenderer<GitlabVisibility?>() {
                    override fun customize(list: JList<out GitlabVisibility?>, value: GitlabVisibility?, index: Int, selected: Boolean, hasFocus: Boolean) {
                        text = if (value != null) StringUtil.capitalize(value.name.lowercase()) else ""
                    }
                }).bindItem({ selectedVisibility }, { selectedVisibility = it ?: GitlabVisibility.PRIVATE })
                label("").visible(false)
            }
            row(label = GitlabBundle.message("share.dialog.remote.label")) {
                textField().bindText(::gitRemote).validationOnApply {
                    if (it.text.isBlank()) {
                        return@validationOnApply error(GitlabBundle.message("share.validation.no.remote.name"))
                    }
                    val gitRepositoryManager = project.service<GitRepositoryManager>()
                    val moduleRootDirFile = VfsUtil.findFile(Path.of(rootDirTextField.text), false)
                    val repositoryForRoot = gitRepositoryManager.getRepositoryForRootQuick(moduleRootDirFile)
                    if (repositoryForRoot?.remotes?.any { remote -> remote.name == it.text } == true) {
                        return@validationOnApply error(GitlabBundle.message("share.error.remote.with.selected.name.exists"))
                    }
                    return@validationOnApply null
                }
                // Just to add a third component to make the table design correct. Table design with 3 columns. Here, we have no component to show, but without a component the grid is broken: only two
                // cells are rendered requiring the entire width. Means, the component above is taking the entire space and this doesn't look good.
                label("").visible(false)
            }
            row(label = GitlabBundle.message("share.dialog.root.dir")) {
                rootDirTextField = textFieldWithBrowseButton(browseDialogTitle = GitlabBundle.message("share.dialog.root.dir.browse.dialog.title"),
                                                             project = project,
                                                             fileChooserDescriptor = FileChooserDescriptor(false, true, false, false, false, false)).bindText(::rootDir).validationOnApply {
                    if (it.text.isBlank()) return@validationOnApply error(GitlabBundle.message("share.dialog.missing.module.root.dir"))

                    val localChosenDir = VfsUtil.findFile(Path.of(it.text), false)
                    if (localChosenDir == null || !projectRootManager.fileIndex.isInContent(localChosenDir)) return@validationOnApply error(GitlabBundle.message("share.dialog.invalid.module.root.dir"))
                    rootDirVirtualFile = localChosenDir
                    return@validationOnApply null
                }.component
                label("").visible(false)
            }
            row(label = GitlabBundle.message("share.dialog.description.label")) {
                textField().bindText(::description)
                // Just to add a third component to make the table design correct. Table design with 3 columns. Here, we have no component to show, but without a component the grid is broken: only two
                // cells are rendered requiring the entire width. Means, the component above is taking the entire space and this doesn't look good.
                label("").visible(false)
            }
        }
    }

    init {
        isAutoAdjustable = true
        init()
        title = GitlabBundle.message("share.dialog.module.title")
        centerRelativeToParent()
        setOKButtonText(GitlabBundle.message("share.button"))
    }

    override fun createCenterPanel(): JComponent = centerPanel

    @RequiresEdt
    fun fillWithDefaultValues(gitlabAccounts: List<GitlabAccount>) {
        projectName = module?.name ?: project.name
        rootDir = module?.guessModuleDir()?.path ?: project.guessProjectDir()?.path ?: ""
        accountComboBoxModel.removeAll()
        accountComboBoxModel.add(gitlabAccounts)
        if (gitlabAccounts.isNotEmpty()) {
            selectedAccount = gitlabAccounts[0]
        }
        // Reset to trigger a read from the property bindings defined above via Kotlin UI DSL. Else, the components wouldn't know there are default values to use and simply don't show them.
        centerPanel.reset()
        pack()
    }

    fun constructProjectLinkerConfiguration(): ProjectLinkingConfiguration =
            ProjectLinkingConfiguration(projectName, rootDirVirtualFile!!, gitRemote, selectedVisibility, projectNamespaceId, description, selectedAccount!!)
}