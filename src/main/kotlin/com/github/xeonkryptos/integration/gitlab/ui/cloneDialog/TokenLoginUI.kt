package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import javax.swing.event.DocumentEvent

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class TokenLoginUI(project: Project) {

    private val gitlabSettings = GitlabSettingsService.getInstance(project).state

    val gitlabHostTxtField: JBTextField = JBTextField().apply {
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (!disableCertificateValidationManuallySet) {
                    @Suppress("HttpUrlsUsage")
                    val skipCharacters = if (text.startsWith("http://")) "http://".length else if (text.startsWith("https://")) "https://".length else 0
                    val endOfHostname = text.indexOf('/', skipCharacters)
                    val domainWithProtocol = if (endOfHostname > -1) text.substring(0 until endOfHostname) else text

                    disableCertificateValidation = gitlabSettings.gitlabHostSettings[domainWithProtocol]?.disableSslVerification ?: false
                    if (checkBoxBuilder?.component?.isSelected != disableCertificateValidation) {
                        checkBoxBuilder?.component?.isSelected = disableCertificateValidation
                    }
                }
            }
        })
    }
    private val gitlabAccessTokenTxtField: JBTextField = JBTextField()

    private var checkBoxBuilder: CellBuilder<JBCheckBox>? = null
    private var disableCertificateValidationManuallySet: Boolean = false
    var disableCertificateValidation: Boolean = false

    val tokenLoginPanel: DialogPanel

    init {
        tokenLoginPanel = panel(title = GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.text")) {
            row(GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.host")) {
                gitlabHostTxtField().applyIfEnabled().focused().withValidationOnApply { validateHostField() }
            }
            row(GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.token")) {
                gitlabAccessTokenTxtField().withValidationOnApply { validateTokenField() }
            }
            row {
                checkBoxBuilder = checkBox(GitlabBundle.message("settings.general.table.column.certificates"), this@TokenLoginUI::disableCertificateValidation).applyToComponent {
                    addActionListener { disableCertificateValidationManuallySet = true }
                }
            }
        }
    }

    @Suppress("HttpUrlsUsage")
    private fun ValidationInfoBuilder.validateHostField(): ValidationInfo? {
        if (gitlabHostTxtField.text.isBlank()) {
            return error(GitlabBundle.message("credentials.server.cannot.be.empty"))
        }
        if (!gitlabHostTxtField.text.startsWith("http://") && !gitlabHostTxtField.text.startsWith("https://")) {
            return error(GitlabBundle.message("credentials.server.path.invalid"))
        }
        return null
    }

    private fun ValidationInfoBuilder.validateTokenField(): ValidationInfo? {
        if (gitlabAccessTokenTxtField.text.isBlank()) {
            return error(GitlabBundle.message("credentials.token.cannot.be.empty"))
        }
        return null
    }

    fun getGitlabLoginData() = GitlabLoginData(gitlabHostTxtField.text, gitlabAccessTokenTxtField.text, disableCertificateValidation)
}
