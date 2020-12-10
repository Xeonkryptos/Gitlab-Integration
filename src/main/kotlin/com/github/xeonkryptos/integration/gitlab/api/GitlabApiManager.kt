package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProjectWrapper
import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService
import com.github.xeonkryptos.integration.gitlab.storage.GitlabCredentials
import com.intellij.openapi.project.Project
import java.awt.image.BufferedImage
import java.net.URL
import java.util.stream.Collectors
import javax.imageio.ImageIO
import org.gitlab4j.api.GitLabApi

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class GitlabApiManager(project: Project) {

    private val dataService = GitlabDataService.getInstance(project)

    private var gitLabApi: GitLabApi? = null

    init {
        openGitlabApiAccess()
    }

    fun retrieveProjects(): List<GitlabProjectWrapper> {
        val localGitlabApi = openGitlabApiAccess()
        if (localGitlabApi != null) {
            return localGitlabApi.projectApi.ownedProjectsStream.map { GitlabProjectWrapper(it) }.collect(Collectors.toList())
        }
        return emptyList()
    }

    private fun openGitlabApiAccess(): GitLabApi? {
        val gitlabHost = dataService.state?.activeGitlabHost
        if (gitlabHost != null) {
            switchToGitlabHost(gitlabHost)
        }
        return gitLabApi
    }

    fun switchToGitlabHost(newGitlabHost: String) {
        gitLabApi?.close()

        val targetGitlabHost = if (newGitlabHost.endsWith("/")) newGitlabHost.substring(0, newGitlabHost.length - 1); else newGitlabHost
        val gitlabAccessToken = GitlabCredentials.getTokenFor(targetGitlabHost)
        if (gitlabAccessToken != null) {
            gitLabApi = GitLabApi(targetGitlabHost, gitlabAccessToken)
        } else {
            // TODO: Log message for missing access token but stored gitlab host... Maybe allow setting it in settings?
        }
    }

    fun getGitlabServerUrl() = gitLabApi?.gitLabServerUrl

    fun getAvatarImage(): BufferedImage? {
        val avatarUrl = gitLabApi?.userApi?.currentUser?.avatarUrl
        return if (avatarUrl != null) {
            val convertedUrl = URL(avatarUrl)
            ImageIO.read(convertedUrl)
        } else null
    }
}
