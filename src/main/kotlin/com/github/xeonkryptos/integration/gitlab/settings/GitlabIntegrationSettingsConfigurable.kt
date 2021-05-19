package com.github.xeonkryptos.integration.gitlab.settings

import com.github.xeonkryptos.integration.gitlab.settings.ui.GitlabIntegrationSettingsForm
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

/**
 * @author Xeonkryptos
 * @since 15.02.2021
 */
class GitlabIntegrationSettingsConfigurable(project: Project) : Configurable {

    private val settingsForm: GitlabIntegrationSettingsForm by lazy { GitlabIntegrationSettingsForm(project) }

    override fun createComponent(): JComponent = settingsForm.gitlabTablePanel

    override fun isModified(): Boolean {
        return settingsForm.isModified
    }

    override fun apply() {
        settingsForm.apply()
    }

    override fun getDisplayName() = "Gitlab Integrations"

    override fun reset() {
        settingsForm.reset()
    }

    override fun disposeUIResources() {
        Disposer.dispose(settingsForm)
    }
}
