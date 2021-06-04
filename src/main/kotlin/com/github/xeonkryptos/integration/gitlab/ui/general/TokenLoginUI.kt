package com.github.xeonkryptos.integration.gitlab.ui.general

import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.ui.clone.GitlabLoginData
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import java.net.URI
import javax.swing.event.DocumentEvent

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class TokenLoginUI(withPanelTitle: Boolean = true) {

    private val gitlabSettings = service<GitlabSettingsService>().state

    val gitlabHostTxtField: JBTextField = JBTextField().apply {
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (!disableCertificateValidationManuallySet) {
                    try {
                        val gitlabDomain = URI(text).host
                        disableCertificateValidation = gitlabSettings.gitlabHostSettings[gitlabDomain]?.disableSslVerification ?: false
                        if (checkBoxBuilder?.component?.isSelected != disableCertificateValidation) {
                            checkBoxBuilder?.component?.isSelected = disableCertificateValidation
                        }
                    } catch (ignored: Exception) {}
                }
            }
        })
    }
    private val gitlabAccessTokenTxtField: JBTextField = JBTextField()

    private var checkBoxBuilder: CellBuilder<JBCheckBox>? = null
    private var disableCertificateValidationManuallySet: Boolean = false
    var disableCertificateValidation: Boolean = false

    val dialogPanel: DialogPanel

    init {
        dialogPanel = panel(title = if (withPanelTitle) GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.text") else null) {
            row(GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.host")) {
                gitlabHostTxtField().focused().withValidationOnApply { validateHostField() }
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
        dialogPanel.withMaximumHeight(dialogPanel.preferredSize.height)
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
