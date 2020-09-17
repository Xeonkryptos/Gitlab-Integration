package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService
import com.github.xeonkryptos.integration.gitlab.storage.GitlabCredentials
import com.intellij.openapi.project.Project

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class GitlabProjectApi(project: Project) {

    private val dataService = GitlabDataService.getInstance(project)

    fun retrieveProjectList(): List<GitlabProject> {
        val gitlabHosts = dataService.gitlabData?.gitlabHosts
        if (gitlabHosts != null && gitlabHosts.isNotEmpty()) {
            val gitlabProjects = ArrayList<GitlabProject>()
            gitlabHosts.forEach { gitlabHost ->
                val gitlabAccessToken = GitlabCredentials.getTokenFor(gitlabHost)
                if (gitlabAccessToken != null) {
                    // TODO: Load list of modules from gitlab host with loaded access token
                } else {
                    // TODO: Log message for missing access token but stored gitlab host... Maybe allow setting it in settings?
                }
            }
            return gitlabProjects
        }
        return emptyList()
    }
}
