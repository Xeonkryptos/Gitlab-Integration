package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import javax.swing.JComponent

class ProjectLinkerDialog(project: Project) : DialogWrapper(project) {

    private val accountComboBoxModel: CollectionComboBoxModel<GitlabAccount> = CollectionComboBoxModel()
    private var selectedAccount: GitlabAccount? = null

    private var projectPath: String = ""
    private var projectName: String = ""
    private var gitRemote: String = "origin"
    private var description: String = ""

    private var privateProjectSelected: Boolean = false

    init {
        init()
        title = GitlabBundle.message("share.dialog.module.title")
        centerRelativeToParent()
        setOKButtonText(GitlabBundle.message("share.button"))
        horizontalStretch = 1.3f
    }

    override fun createCenterPanel(): JComponent {
        return panel(title = GitlabBundle.message("share.dialog.module.title")) {
            row(label = GitlabBundle.message("share.dialog.account.label")) {
                comboBox(accountComboBoxModel, { selectedAccount }, { selectedAccount = it }).withValidationOnApply {
                    if (it.selectedItem != null) {
                        return@withValidationOnApply error(GitlabBundle.message("share.dialog.missing.account"))
                    }
                    return@withValidationOnApply null
                }.applyToComponent { addItemListener { projectPath = "" } }
                link(text = GitlabBundle.message("share.dialog.manage.accounts.link")) {
                    TODO("Implement action")
                }
            }
            row(label = GitlabBundle.message("share.dialog.project.path.label")) {
                textField(::projectPath)
                link(text = GitlabBundle.message("share.dialog.group.choose.link")) {
                    TODO("Implement action")
                }
            }
            row(label = GitlabBundle.message("share.dialog.project.name.label")) {
                textField(::projectName).withValidationOnApply {
                    return@withValidationOnApply if (it.text.isBlank()) error(GitlabBundle.message("share.dialog.missing.project.name")) else null
                }
                checkBox(text = GitlabBundle.message("share.dialog.project.name.private.checkbox"), ::privateProjectSelected)
            }
            row(label = GitlabBundle.message("share.dialog.remote.label")) {
                textField(::gitRemote).withValidationOnApply {
                    return@withValidationOnApply if (it.text.isBlank()) error(GitlabBundle.message("share.dialog.missing.remote.name")) else null
                }
            }
            row(label = GitlabBundle.message("share.dialog.description.label")) {
                textField(::description)
            }
        }
    }

    @RequiresEdt
    fun fillWithDefaultValues(moduleName: String?, gitlabAccounts: List<GitlabAccount>) {
        projectName = moduleName ?: ""
        accountComboBoxModel.removeAll()
        accountComboBoxModel.add(gitlabAccounts)
        if (gitlabAccounts.isNotEmpty()) {
            accountComboBoxModel.selectedItem = gitlabAccounts[0]
        }
        accountComboBoxModel.update()
    }
}