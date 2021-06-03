package com.github.xeonkryptos.integration.gitlab.ui.clone

import com.github.xeonkryptos.integration.gitlab.ui.general.ErrorText
import com.github.xeonkryptos.integration.gitlab.ui.general.LoginTask
import com.github.xeonkryptos.integration.gitlab.ui.general.TokenLoginUI
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.ContainerUtil
import java.util.Collections
import java.util.EventListener
import java.util.EventObject
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.EventListenerList

class EmbeddedGitlabLoginPanel(project: Project) : JPanel(), Disposable {

    private val eventListeners = EventListenerList()

    private val tokenLoginUI: TokenLoginUI = TokenLoginUI()
    private val errorText: ErrorText = ErrorText()
    private val loginButton: JButton = JButton(GitlabBundle.message("accounts.log.in")).apply {
        addActionListener {
            val validationInfo: List<ValidationInfo> = tokenLoginUI.dialogPanel.validateCallbacks.mapNotNull { it() }
            if (validationInfo.isEmpty()) {
                val gitlabLoginData: GitlabLoginData = tokenLoginUI.getGitlabLoginData()
                LoginTask(project, gitlabLoginData) { result ->
                    SwingUtilities.invokeLater {
                        if (result == null) {
                            val event = GitlabLoginEvent(this@EmbeddedGitlabLoginPanel)
                            eventListeners.getListeners(GitlabLoginActionListener::class.java).forEach { it.onSuccessfulLogin(event) }
                        } else {
                            val updatedInfo: MutableList<ValidationInfo> = ArrayList(currentValidationInfo)
                            updatedInfo.add(ValidationInfo(result))

                            updateValidationState(updatedInfo)
                        }
                    }
                }.doLogin()
            } else {
                updateValidationState(validationInfo)
            }
        }
    }

    var currentValidationInfo: List<ValidationInfo> = Collections.emptyList()
        private set

    init {
        Disposer.register(this, tokenLoginUI)

        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(tokenLoginUI.dialogPanel)
        add(errorText)
        add(loginButton)
    }

    private fun updateValidationState(validationInfo: List<ValidationInfo>) {
        if (validationInfo == currentValidationInfo) return

        resetValidationState(validationInfo)
        validationInfo.forEach {
            val currentComponent = it.component
            if (currentComponent != null) {
                ComponentValidator.getInstance(currentComponent).orElseGet { ComponentValidator(this@EmbeddedGitlabLoginPanel).installOn(currentComponent) }.updateInfo(it)
            } else {
                errorText.appendError(it)
            }
        }
    }

    private fun resetValidationState(validationInfo: List<ValidationInfo>) {
        errorText.clearError(ContainerUtil.all(validationInfo) { i: ValidationInfo -> StringUtil.isEmpty(i.message) })
        currentValidationInfo.asSequence()
            .filter { !validationInfo.contains(it) && it.component != null }
            .mapNotNull { ComponentValidator.getInstance(it.component!!).orElse(null) }
            .forEach { it.updateInfo(null) }
    }

    fun addGitlabLoginActionListener(listener: GitlabLoginActionListener) {
        eventListeners.add(GitlabLoginActionListener::class.java, listener)
    }

    override fun dispose() {}

    interface GitlabLoginActionListener : EventListener {

        fun onSuccessfulLogin(event: GitlabLoginEvent)
    }

    class GitlabLoginEvent(source: Any) : EventObject(source)
}