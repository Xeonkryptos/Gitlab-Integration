package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import javax.swing.JComponent

class ProjectLinkerDialog(project: Project) : DialogWrapper(project) {

    private val accountComboBoxModel: CollectionComboBoxModel<GitlabAccount> = CollectionComboBoxModel()
    private var selectedAccount: GitlabAccount? = null

    // Properties need to be public because they are accessed via property binding and exactly this binding can only access publicly available properties.
    var projectPath: String = ""
    var projectName: String = ""
    var gitRemote: String = "origin"
    var description: String = ""
    var privateProjectSelected: Boolean = false

    private val centerPanel: DialogPanel by lazy {
        panel(title = GitlabBundle.message("share.dialog.module.title")) {
            row(label = GitlabBundle.message("share.dialog.account.label")) {
                comboBox(accountComboBoxModel, { selectedAccount }, { selectedAccount = it }).withValidationOnApply {
                    if (it.selectedItem != null) {
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
                checkBox(text = GitlabBundle.message("share.dialog.project.name.private.checkbox"), ::privateProjectSelected).withLargeLeftGap()
            }
            row(label = GitlabBundle.message("share.dialog.remote.label")) {
                textField(::gitRemote).withValidationOnApply {
                    return@withValidationOnApply if (it.text.isBlank()) error(GitlabBundle.message("share.dialog.missing.remote.name")) else null
                }
                // Just to add a third component to make the table design correct. Table design with 3 columns. Here, we have no component to show, but without a component the grid is broken: only two
                // cells are rendered requiring the entire width. Means, the component above is taking the entire space and this doesn't look good.
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
        setAutoAdjustable(true)
        init()
        title = GitlabBundle.message("share.dialog.module.title")
        centerRelativeToParent()
        setOKButtonText(GitlabBundle.message("share.button"))
    }

    override fun createCenterPanel(): JComponent = centerPanel

    @RequiresEdt
    fun fillWithDefaultValues(moduleName: String?, gitlabAccounts: List<GitlabAccount>) {
        projectName = moduleName ?: ""
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