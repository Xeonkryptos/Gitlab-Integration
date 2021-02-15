package com.github.xeonkryptos.integration.gitlab.service

import com.github.xeonkryptos.integration.gitlab.service.settings.GitlabIntegrationSettings
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * @author Xeonkryptos
 * @since 15.02.2021
 */
@State(name = "gitlab-settings", reloadable = true)
class GitlabIntegrationSettingsService(@Suppress("UNUSED_PARAMETER") project: Project) : PersistentStateComponent<GitlabIntegrationSettings> {

    companion object {

        @JvmStatic
        private var instance: GitlabIntegrationSettingsService? = null

        @JvmStatic
        @Synchronized
        fun getInstance(project: Project): GitlabIntegrationSettingsService {
            if (instance == null) {
                instance = project.service()
            }
            return instance!!
        }
    }

    private var settings = GitlabIntegrationSettings()

    override fun getState(): GitlabIntegrationSettings = settings

    override fun loadState(state: GitlabIntegrationSettings) {
        settings = state
    }
}
