package com.github.xeonkryptos.integration.gitlab.settings.ui

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.LoginTask
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.TokenLoginUI
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent
import javax.swing.SwingUtilities

class AddGitlabSettingsEntryDialog(private val project: Project) : DialogWrapper(project, true, IdeModalityType.IDE) {

    private val tokenLoginUI: TokenLoginUI = TokenLoginUI(withPanelTitle = false)

    var gitlabAccount: GitlabAccount? = null
        private set

    init {
        Disposer.register(disposable, tokenLoginUI)
        init()
        title = GitlabBundle.message("settings.general.dialog.add")
        horizontalStretch = 1.5f
        centerRelativeToParent()
        setOKButtonText(GitlabBundle.message("accounts.log.in"))

        rootPane.defaultButton = getButton(okAction)
        ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, object : GitlabLoginChangeNotifier {
            override fun onSignIn(gitlabAccount: GitlabAccount) {
                val action = Runnable { isOKActionEnabled = true }
                if (SwingUtilities.isEventDispatchThread()) {
                    action.run()
                } else {
                    SwingUtilities.invokeLater(action)
                }
            }

            override fun onSignOut(gitlabAccount: GitlabAccount) {}
        })
    }

    override fun createCenterPanel(): JComponent {
        return tokenLoginUI.tokenLoginPanel
    }

    override fun doOKAction() {
        val gitlabLoginData = tokenLoginUI.getGitlabLoginData()
        LoginTask(project, gitlabLoginData) { result: String? ->
            setErrorText(result)
            if (result == null) {
                // Because of the modality behaviour, ApplicationManager.invokeLater() can't be used.
                SwingUtilities.invokeLater { close(OK_EXIT_CODE) }
            }
        }.doLogin()
    }
}