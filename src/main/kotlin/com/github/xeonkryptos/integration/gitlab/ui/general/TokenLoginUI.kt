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
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import java.net.URI
import javax.swing.event.DocumentEvent

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class TokenLoginUI {

    private val gitlabSettings = service<GitlabSettingsService>().getWorkableState()

    private lateinit var gitlabHostTxtField: JBTextField
    private lateinit var gitlabAccessTokenTxtField: JBTextField

    private var checkBoxBuilder: Cell<JBCheckBox>? = null
    private var disableCertificateValidationManuallySet: Boolean = false
    var disableCertificateValidation: Boolean = false

    val dialogPanel: DialogPanel

    init {
        dialogPanel = panel {
            row(GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.host")) {
                gitlabHostTxtField = textField().applyToComponent {
                    document.addDocumentListener(object : DocumentAdapter() {
                        override fun textChanged(e: DocumentEvent) {
                            if (!disableCertificateValidationManuallySet) {
                                try {
                                    val gitlabDomain = URI(text).host
                                    disableCertificateValidation = gitlabSettings.gitlabHostSettings[gitlabDomain]?.disableSslVerification ?: false
                                    if (checkBoxBuilder?.component?.isSelected != disableCertificateValidation) {
                                        checkBoxBuilder?.component?.isSelected = disableCertificateValidation
                                    }
                                } catch (ignored: Exception) {
                                }
                            }
                        }
                    })
                }.focused().validationOnApply { validateHostField() }.component
            }
            row(GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.token")) {
                gitlabAccessTokenTxtField = textField().resizableColumn().validationOnApply { validateTokenField() }.component
            }
            row {
                checkBoxBuilder = checkBox(GitlabBundle.message("settings.general.table.column.certificates")).bindSelected(this@TokenLoginUI::disableCertificateValidation).applyToComponent {
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
