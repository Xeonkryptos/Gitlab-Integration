package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService
import com.github.xeonkryptos.integration.gitlab.storage.GitlabCredentials
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.panel

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class TokenLoginUI(project: Project, private val onLoginAction: Runnable) {

    private val dataService = GitlabDataService.getInstance(project)

    private val gitlabHostTxtField: JBTextField = JBTextField()
    private val gitlabAccessTokenTxtField: JBTextField = JBTextField()

    val tokenLoginPanel: DialogPanel

    init {
        tokenLoginPanel = panel(title = "Gitlab Login via Token") {
            row("Gitlab Host: ") { gitlabHostTxtField().applyIfEnabled().focused() }
            row("Gitlab Token: ") { gitlabAccessTokenTxtField().applyIfEnabled() }
            row {
                button("Log in") {
                    val gitlabHost = gitlabHostTxtField.text
                    val gitlabAccessToken = gitlabAccessTokenTxtField.text

                    // TODO: Make access check before store credentials and call listener
                    dataService.state?.activeGitlabHost = gitlabHost
                    GitlabCredentials.storeTokenFor(gitlabHost, gitlabAccessToken)
                    onLoginAction.run()
                }.enableIf(AccessTokenLoginPredicate())
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
