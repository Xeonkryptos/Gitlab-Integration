package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import java.awt.Color

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class TokenLoginUI(project: Project, private val gitlabApiManager: GitlabApiManager, private val onLoginAction: () -> Unit) {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val authenticationManager = AuthenticationManager.getInstance(project)

    private val gitlabSettings = GitlabSettingsService.getInstance(project).state

    private val gitlabHostTxtField: JBTextField = JBTextField()
    private val gitlabAccessTokenTxtField: JBTextField = JBTextField()

    private var errorRow: Row? = null

    val tokenLoginPanel: DialogPanel

    var onSwitchLoginMethod: (() -> Unit)? = null

    init {
        val errorLabel = JBLabel().apply {
            setAllowAutoWrapping(true)
            foreground = Color.RED
        }

        tokenLoginPanel = panel(title = "Gitlab Login via Token") {
            row("Gitlab Host: ") { gitlabHostTxtField().applyIfEnabled().focused() }
            row("Gitlab Token: ") { gitlabAccessTokenTxtField().applyIfEnabled() }
            row {
                button("Log in") {
                    val gitlabHost = gitlabHostTxtField.text
                    val gitlabAccessToken = gitlabAccessTokenTxtField.text

                    try {
                        val gitlabHostSettings = gitlabSettings.getOrCreateGitlabHostSettings(gitlabHost)
                        val gitlabUser = gitlabApiManager.loadGitlabUser(gitlabHostSettings, gitlabAccessToken)

                        val gitlabAccount = gitlabHostSettings.createGitlabAccount(gitlabUser.username)
                        authenticationManager.storeAuthentication(gitlabAccount, gitlabAccessToken)

                        onLoginAction.invoke()
                    } catch (e: Exception) {
                        LOG.error("Log in with provided access token failed.", e, "Host: $gitlabHost")
                        errorLabel.text = "Log in failed. Reason: ${e.message}"
                        errorRow?.visible = true
                    }
                }.enableIf(AccessTokenLoginPredicate())
            }
            row {
                link("Login via username and password") { onSwitchLoginMethod?.invoke() }
            }
            errorRow = row(errorLabel) {
                visible = false
            }
        }
    }

    internal inner class AccessTokenLoginPredicate : ComponentPredicate() {

        override fun addListener(listener: (Boolean) -> Unit) {
            gitlabHostTxtField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: javax.swing.event.DocumentEvent) {
                    listener(invoke())
                }
            })
            gitlabAccessTokenTxtField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: javax.swing.event.DocumentEvent) {
                    listener(invoke())
                }
            })
        }

        override fun invoke(): Boolean = gitlabHostTxtField.text.isNotBlank() && gitlabAccessTokenTxtField.text.isNotBlank()
    }
}
