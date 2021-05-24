package com.github.xeonkryptos.integration.gitlab.ui.general

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent
import javax.swing.SwingUtilities

class AddGitlabAccountEntryDialog(private val project: Project) : DialogWrapper(project, true, IdeModalityType.IDE) {

    private val tokenLoginUI: TokenLoginUI = TokenLoginUI(withPanelTitle = false)

    init {
        Disposer.register(disposable, tokenLoginUI)
        init()
        title = GitlabBundle.message("settings.general.dialog.add")
        horizontalStretch = 1.5f
        centerRelativeToParent()
        setOKButtonText(GitlabBundle.message("accounts.log.in"))

        rootPane.defaultButton = getButton(okAction)
    }

    override fun createCenterPanel(): JComponent {
        return tokenLoginUI.tokenLoginPanel
    }

    override fun doOKAction() {
        val gitlabLoginData = tokenLoginUI.getGitlabLoginData()
        LoginTask(project, gitlabLoginData) { result: String? ->
            setErrorText(result)
            if (result == null) SwingUtilities.invokeLater { close(OK_EXIT_CODE) }
        }.doLogin()
    }
}