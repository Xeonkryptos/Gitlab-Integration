package com.github.xeonkryptos.integration.gitlab.service

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
@State(name = "gitlab-settings", reloadable = true)
class GitlabSettingsService(@Suppress("UNUSED_PARAMETER") project: Project) : PersistentStateComponent<GitlabSettings> {

    companion object {

        @JvmStatic
        private var instance: GitlabSettingsService? = null

        @JvmStatic
        @Synchronized
        fun getInstance(project: Project): GitlabSettingsService {
            if (instance == null) {
                instance = project.service()
            }
            return instance!!
        }
    }

    private var gitlabData = GitlabSettings()

    override fun getState(): GitlabSettings = gitlabData

    override fun loadState(state: GitlabSettings) {
        state.onLoadingFinished()
        gitlabData = state
    }
}
