package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService
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
class OAuthLoginUI(project: Project, private val gitlabApiManager: GitlabApiManager, private val onLoginAction: Runnable) {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val authenticationManager = AuthenticationManager.getInstance(project)

    private val gitlabDataService = GitlabDataService.getInstance(project)

    private val gitlabHostTxtField: JBTextField = JBTextField()
    private val gitlabUsernameTxtField: JBTextField = JBTextField()
    private val gitlabPasswordTxtField: JBTextField = JBTextField()

    private var errorRow: Row? = null

    val oauthLoginPanel: DialogPanel

    var onSwitchLoginMethod: (() -> Unit)? = null

    init {
        val errorLabel = JBLabel().apply {
            setAllowAutoWrapping(true)
            foreground = Color.RED
        }

        oauthLoginPanel = panel(title = "Gitlab Login") {
            row("Gitlab Host: ") { gitlabHostTxtField().applyIfEnabled().focused() }
            row("Username: ") { gitlabUsernameTxtField().applyIfEnabled() }
            row("Password: ") { gitlabPasswordTxtField().applyIfEnabled() }
            row {
                button("Log in") {
                    val gitlabHost = gitlabHostTxtField.text
                    val gitlabUsername = gitlabUsernameTxtField.text
                    val gitlabPassword = gitlabPasswordTxtField.text

                    try {
                        val gitlabUser = gitlabApiManager.loadGitlabUser(gitlabHost, gitlabUsername, gitlabPassword)

                        authenticationManager.storeAuthenticationPassword(gitlabUser.gitlabAccount, gitlabPassword)
                        gitlabDataService.state.activeGitlabAccount = gitlabUser.gitlabAccount

                        onLoginAction.run()
                    } catch (e: Exception) {
                        LOG.error("Log in with provided username password combination failed.", e, "Host: $gitlabHost", "Username: $gitlabUsername")
                        errorLabel.text = "Log in failed. Reason: ${e.message}"
                        errorRow?.visible = true
                    }
                }.enableIf(AccessTokenLoginPredicate())
            }
            row {
                link("Login via token") { onSwitchLoginMethod?.invoke() }
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
            gitlabUsernameTxtField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: javax.swing.event.DocumentEvent) {
                    listener(invoke())
                }
            })
            gitlabPasswordTxtField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: javax.swing.event.DocumentEvent) {
                    listener(invoke())
                }
            })
        }

        override fun invoke(): Boolean = gitlabHostTxtField.text.isNotBlank() && gitlabUsernameTxtField.text.isNotBlank() && gitlabPasswordTxtField.text.isNotBlank()
    }
}
