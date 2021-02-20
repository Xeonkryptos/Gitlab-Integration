package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import java.awt.Color
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

    private val gitlabHostTxtField: JBTextField = JBTextField()
    private val gitlabAccessTokenTxtField: JBTextField = JBTextField()

    private var errorRow: Row? = null

    val tokenLoginPanel: DialogPanel

    init {
        val errorLabel = JBLabel().apply {
            setAllowAutoWrapping(true)
            foreground = Color.RED
        }

        tokenLoginPanel = panel(title = "Gitlab Login via Token") {
            row("Gitlab Host: ") { gitlabHostTxtField().applyIfEnabled().focused() }
            row("Gitlab Token: ") { gitlabAccessTokenTxtField().applyIfEnabled().onApply { println("Test") } }
            row {
                button("Log in") {
                    val gitlabHost = gitlabHostTxtField.text
                    val gitlabAccessToken = gitlabAccessTokenTxtField.text

                    try {
                        val gitlabHostSettings = gitlabSettings.getOrCreateGitlabHostSettings(gitlabHost)
                        val backgroundTask = object : Task.Backgroundable(project, "Downloading user information", false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                            override fun run(indicator: ProgressIndicator) {
                                val gitlabUser = gitlabApiManager.loadGitlabUser(gitlabHostSettings, gitlabAccessToken)
                                val gitlabAccount = gitlabHostSettings.createGitlabAccount(gitlabUser.username)
                                authenticationManager.storeAuthentication(gitlabAccount, gitlabAccessToken)
                            }
                        }
                        ProgressManager.getInstance().runProcessWithProgressAsynchronously(backgroundTask, EmptyProgressIndicator(ModalityState.NON_MODAL))
                    } catch (e: Exception) {
                        LOG.error("Log in with provided access token failed.", e, "Host: $gitlabHost")
                        errorLabel.text = "Log in failed. Reason: ${e.message}"
                        errorRow?.visible = true
                    }
                }.enableIf(AccessTokenLoginPredicate())
            }
            errorRow = row(errorLabel) {
                visible = false
            }
        }
    }

    private inner class AccessTokenLoginPredicate : ComponentPredicate() {

        override fun addListener(listener: (Boolean) -> Unit) {
            gitlabHostTxtField.document.addDocumentListener(createDocumentChangeListener(listener))
            gitlabAccessTokenTxtField.document.addDocumentListener(createDocumentChangeListener(listener))
        }

        private fun createDocumentChangeListener(listener: (Boolean) -> Unit): DocumentListener = object : DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                listener(invoke())
            }
        }

        override fun invoke(): Boolean = gitlabHostTxtField.text.isNotBlank() && gitlabAccessTokenTxtField.text.isNotBlank()
    }
}
