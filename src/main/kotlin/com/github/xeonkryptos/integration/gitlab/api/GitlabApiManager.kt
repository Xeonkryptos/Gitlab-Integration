package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import org.gitlab4j.api.GitLabApi
import java.util.stream.Collectors

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class GitlabApiManager(project: Project) {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val gitlabSettingsService = GitlabSettingsService.getInstance(project)
    private val authenticationManager = AuthenticationManager.getInstance(project)

    fun retrieveGitlabUsersFor(gitlabAccounts: Collection<GitlabAccount>): Map<GitlabAccount, GitlabUser> {
        val users = mutableMapOf<GitlabAccount, GitlabUser>()
        gitlabAccounts.filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            var gitlabApi: GitLabApi? = null
            try {
                gitlabApi = openGitlabApiAccess(gitlabAccount)
                users[gitlabAccount] = GitlabUser(gitlabApi.userApi.currentUser)
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve user information for gitlab account $gitlabAccount", e)
            } finally {
                gitlabApi?.close()
            }
        }
        return users
    }

    fun retrieveGitlabProjectsFor(gitlabAccounts: Collection<GitlabAccount>): Map<GitlabAccount, List<GitlabProject>> {
        val accountProjects = mutableMapOf<GitlabAccount, List<GitlabProject>>()
        gitlabAccounts.filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            var gitlabApi: GitLabApi? = null
            try {
                gitlabApi = openGitlabApiAccess(gitlabAccount)
                val projects = if (gitlabAccount.resolveOnlyOwnProjects) gitlabApi.projectApi.ownedProjectsStream else gitlabApi.projectApi.projectsStream
                accountProjects[gitlabAccount] = projects.map { GitlabProject(it, gitlabAccount) }.collect(Collectors.toList())
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve project information for gitlab account $gitlabAccount", e)
            } finally {
                gitlabApi?.close()
            }
        }
        return accountProjects
    }

    fun loadGitlabUser(gitlabHostSettings: GitlabHostSettings, accessToken: String): GitlabUser {
        var gitlabApi: GitLabApi? = null
        try {
            gitlabApi = GitLabApi(gitlabHostSettings.gitlabHost, accessToken).apply { ignoreCertificateErrors = gitlabHostSettings.disableSslVerification }

            val gitlabUser = gitlabApi.userApi.currentUser
            return GitlabUser(gitlabUser)
        } finally {
            gitlabApi?.close()
        }
    }

    private fun openGitlabApiAccess(gitlabAccount: GitlabAccount): GitLabApi {
        val gitlabHost = gitlabAccount.getGitlabHost()
        val username = gitlabAccount.username

        val targetGitlabHost = if (gitlabHost.endsWith("/")) gitlabHost.substring(0, gitlabHost.length - 1); else gitlabHost
        val gitlabAccessToken by lazy { authenticationManager.getAuthenticationTokenFor(gitlabAccount) }

        if (gitlabAccessToken != null) {
            return GitLabApi(targetGitlabHost, gitlabAccessToken).apply {
                ignoreCertificateErrors = gitlabSettingsService.state.gitlabHostSettings[targetGitlabHost]?.disableSslVerification ?: false
            }
        }
        throw IllegalArgumentException("Cannot access gitlab instance for host $targetGitlabHost with user $username. Missing access token to authenticate")
    }
}
