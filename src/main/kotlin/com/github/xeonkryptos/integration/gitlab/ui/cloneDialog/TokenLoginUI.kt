package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import java.awt.Color
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class TokenLoginUI(project: Project, private val gitlabApiManager: GitlabApiManager) {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val authenticationManager = AuthenticationManager.getInstance(project)

    private val gitlabSettings = GitlabSettingsService.getInstance(project).state

    private val gitlabHostTxtField: JBTextField = JBTextField().apply {
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (!disableCertificateValidationManuallySet) {
                    val skipCharacters =  if (text.startsWith("http://")) "http://".length else if (text.startsWith("https://")) "https://".length else 0
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

    private var errorRow: Row? = null

    private var checkBoxBuilder: CellBuilder<JBCheckBox>? = null
    private var disableCertificateValidationManuallySet: Boolean = false
    var disableCertificateValidation: Boolean = false

    val tokenLoginPanel: DialogPanel

    init {
        val errorLabel = JBLabel().apply {
            setAllowAutoWrapping(true)
            foreground = Color.RED
        }

        tokenLoginPanel = panel(title = "Gitlab Login via Token") {
            row("Gitlab Host: ") { gitlabHostTxtField().applyIfEnabled().focused() }
            row("Gitlab Token: ") { gitlabAccessTokenTxtField() }
            row {
                checkBoxBuilder = checkBox(GitlabBundle.message("settings.general.table.column.certificates"), this@TokenLoginUI::disableCertificateValidation).applyToComponent {
                    addActionListener { disableCertificateValidationManuallySet = true }
                }
            }
            row {
                button("Log in") {
                    errorRow?.visible = false

                    val gitlabHost = gitlabHostTxtField.text
                    val gitlabAccessToken = gitlabAccessTokenTxtField.text

                    val gitlabHostSettings = gitlabSettings.getOrCreateGitlabHostSettings(gitlabHost).apply { disableSslVerification = this@TokenLoginUI.disableCertificateValidation }
                    val backgroundTask = object : Task.Backgroundable(project, "Downloading user information", false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                        override fun run(indicator: ProgressIndicator) {
                            try {
                                val gitlabUser = gitlabApiManager.loadGitlabUser(gitlabHostSettings, gitlabAccessToken)
                                val gitlabAccount = gitlabHostSettings.createGitlabAccount(gitlabUser.username)
                                authenticationManager.storeAuthentication(gitlabAccount, gitlabAccessToken)
                            } catch (e: Exception) {
                                LOG.error("Log in with provided access token failed.", e, "Host: $gitlabHost")
                                ApplicationManager.getApplication().invokeLater {
                                    errorLabel.text = "Log in failed. Reason: ${e.message}"
                                    errorRow?.visible = true
                                }
                            }
                        }
                    }
                    ProgressManager.getInstance().runProcessWithProgressAsynchronously(backgroundTask, EmptyProgressIndicator(ModalityState.NON_MODAL))
                }.enableIf(AccessTokenLoginPredicate())
            }
            errorRow = row(errorLabel) { visible = false }
        }
    }

    private inner class AccessTokenLoginPredicate : ComponentPredicate() {

        override fun addListener(listener: (Boolean) -> Unit) {
            gitlabHostTxtField.document.addDocumentListener(createDocumentChangeListener(listener))
            gitlabAccessTokenTxtField.document.addDocumentListener(createDocumentChangeListener(listener))
        }

        private fun createDocumentChangeListener(listener: (Boolean) -> Unit): DocumentListener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                listener(invoke())
            }
        }

        override fun invoke(): Boolean = gitlabHostTxtField.text.isNotBlank() && gitlabAccessTokenTxtField.text.isNotBlank()
    }
}
