package com.github.xeonkryptos.integration.gitlab.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
@State(name = "gitlab-data", reloadable = true)
class GitlabDataService(project: Project?) : PersistentStateComponent<GitlabData> {

    companion object {

        @JvmStatic
        fun getInstance(project: Project): GitlabDataService {
            return project.service()
        }
    }

    var gitlabData: GitlabData? = null

    override fun getState(): GitlabData? = gitlabData

    override fun loadState(state: GitlabData) {
        gitlabData = state
    }
}
