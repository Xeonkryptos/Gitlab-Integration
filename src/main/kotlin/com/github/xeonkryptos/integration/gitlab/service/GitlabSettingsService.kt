package com.github.xeonkryptos.integration.gitlab.service

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
@State(name = "gitlab-settings", storages = [Storage(value = "gitlab.xml")], reportStatistic = false, reloadable = true)
class GitlabSettingsService : PersistentStateComponent<GitlabSettings> {

    private var gitlabData = GitlabSettings()

    override fun getState(): GitlabSettings = gitlabData

    override fun loadState(state: GitlabSettings) {
        state.onLoadingFinished()
        gitlabData = state
    }
}
