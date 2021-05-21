package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabVisibility
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.repo.GitRepositoryManager
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JList

class ProjectLinkerDialog(private val selectedModule: Module, project: Project) : DialogWrapper(project) {

    private val accountComboBoxModel: CollectionComboBoxModel<GitlabAccount> = CollectionComboBoxModel()
    private val visibilityComboBoxModel: CollectionComboBoxModel<GitlabVisibility> = CollectionComboBoxModel<GitlabVisibility>().apply {
        this.add(GitlabVisibility.values().toList())
    }
    private lateinit var moduleRootDirTextField: TextFieldWithBrowseButton

    // Properties need to be public because they are accessed via property binding and exactly this binding can only access publicly available properties.
    var projectPath: String = ""
    var projectName: String = ""
    var userProject: Boolean = true
    var gitRemote: String = "origin"
    var moduleRootDir: String = ""
    var description: String = ""
    var selectedAccount: GitlabAccount? = null
    var selectedVisibility: GitlabVisibility = GitlabVisibility.PRIVATE

    private val centerPanel: DialogPanel by lazy {
        panel(title = GitlabBundle.message("share.dialog.module.title")) {
            row(label = GitlabBundle.message("share.dialog.account.label")) {
                comboBox(accountComboBoxModel, { selectedAccount }, { selectedAccount = it }).withValidationOnApply {
                    if (it.selectedItem == null) {
                        return@withValidationOnApply error(GitlabBundle.message("share.dialog.missing.account"))
                    }
                    return@withValidationOnApply null
                }.applyToComponent { addItemListener { projectPath = "" } }.constraints(growX)
                link(text = GitlabBundle.message("share.dialog.manage.accounts.link")) {
                    TODO("Implement action")
                }.withLargeLeftGap()
            }
            row(label = GitlabBundle.message("share.dialog.project.path.label")) {
                textField(::projectPath)
                link(text = GitlabBundle.message("share.dialog.group.choose.link")) {
                    TODO("Implement action")
                }.withLargeLeftGap()
            }
            row(label = GitlabBundle.message("share.dialog.project.name.label")) {
                textField(::projectName).withValidationOnApply {
                    return@withValidationOnApply if (it.text.isBlank()) error(GitlabBundle.message("share.dialog.missing.project.name")) else null
                }
                checkBox(text = GitlabBundle.message("share.dialog.user.project.label"), ::userProject).withLargeLeftGap()
            }
            row(label = GitlabBundle.message("share.dialog.project.visibility.label")) {
                comboBox(visibilityComboBoxModel, ::selectedVisibility, renderer = object : SimpleListCellRenderer<GitlabVisibility?>() {
                    override fun customize(list: JList<out GitlabVisibility?>, value: GitlabVisibility?, index: Int, selected: Boolean, hasFocus: Boolean) {
                        text = if (value != null) StringUtil.capitalize(value.name.lowercase()) else ""
                    }
                }).constraints(growX)
                label("").visible(false)
            }
            row(label = GitlabBundle.message("share.dialog.remote.label")) {
                textField(::gitRemote).withValidationOnApply {
                    if (it.text.isBlank()) {
                        return@withValidationOnApply error(GitlabBundle.message("share.dialog.missing.remote.name"))
                    }
                    val gitRepositoryManager = GitRepositoryManager.getInstance(project)
                    val moduleRootDirFile = VfsUtil.findFile(Path.of(moduleRootDirTextField.text), false)
                    val repositoryForRoot = gitRepositoryManager.getRepositoryForRootQuick(moduleRootDirFile)
                    if (repositoryForRoot?.remotes?.any { remote -> remote.name == it.text } == true) {
                        return@withValidationOnApply error(GitlabBundle.message("share.dialog.invalid.remote.name.in.use"))
                    }
                    return@withValidationOnApply null
                }
                // Just to add a third component to make the table design correct. Table design with 3 columns. Here, we have no component to show, but without a component the grid is broken: only two
                // cells are rendered requiring the entire width. Means, the component above is taking the entire space and this doesn't look good.
                label("").visible(false)
            }
            row(label = GitlabBundle.message("share.dialog.module.root.dir")) {
                moduleRootDirTextField = textFieldWithBrowseButton(
                    ::moduleRootDir,
                    browseDialogTitle = GitlabBundle.message("share.dialog.module.root.dir.browse.dialog.title"),
                    project = project,
                    fileChooserDescriptor = FileChooserDescriptor(false, true, false, false, false, false)
                ).withValidationOnApply {
                    if (it.text.isBlank()) {
                        return@withValidationOnApply error(GitlabBundle.message("share.dialog.missing.module.root.dir"))
                    }
                    val moduleRootDirFile = VfsUtil.findFile(Path.of(it.text), false)
                    if (moduleRootDirFile == null || !ModuleUtil.isModuleDir(selectedModule, moduleRootDirFile)) {
                        return@withValidationOnApply error(GitlabBundle.message("share.dialog.invalid.module.root.dir"))
                    }
                    return@withValidationOnApply null
                }.component
                label("").visible(false)
            }
            row(label = GitlabBundle.message("share.dialog.description.label")) {
                textField(::description)
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
        projectName = selectedModule.name
        moduleRootDir = selectedModule.guessModuleDir()?.path ?: ""
        accountComboBoxModel.removeAll()
        accountComboBoxModel.add(gitlabAccounts)
        if (gitlabAccounts.isNotEmpty()) {
            selectedAccount = gitlabAccounts[0]
        }
        // Reset to trigger a read from the property bindings defined above via Kotlin UI DSL. Else, the components wouldn't know there are default values to use and simply don't show them.
        centerPanel.reset()
        pack()
    }
}