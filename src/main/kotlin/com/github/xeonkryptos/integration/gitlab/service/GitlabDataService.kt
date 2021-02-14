package com.github.xeonkryptos.integration.gitlab.service

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabData
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
@State(name = "gitlab-data", reloadable = true)
class GitlabDataService(private val project: Project) : PersistentStateComponent<GitlabData> {

    companion object {

        @JvmStatic
        private var instance: GitlabDataService? = null

        @JvmStatic
        @Synchronized
        fun getInstance(project: Project): GitlabDataService {
            if (instance == null) {
                instance = project.service()
            }
            return instance!!
        }
    }

    private var gitlabData = GitlabData()

    override fun getState(): GitlabData = gitlabData

    override fun loadState(state: GitlabData) {
        state.onLoadingFinished(project)
        gitlabData = state
    }
}
