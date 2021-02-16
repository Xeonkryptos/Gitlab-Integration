package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import java.util.stream.Collectors
import org.gitlab4j.api.GitLabApi

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
        gitlabAccounts.filter { it.signedIn }.forEach { gitlabAccount ->
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
        gitlabAccounts.filter { it.signedIn }.forEach { gitlabAccount ->
            var gitlabApi: GitLabApi? = null
            try {
                gitlabApi = openGitlabApiAccess(gitlabAccount)
                val gitlabProjectsForAccount = gitlabApi.projectApi.ownedProjectsStream.map { GitlabProject(it, gitlabAccount) }.collect(Collectors.toList())
                accountProjects[gitlabAccount] = gitlabProjectsForAccount
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve project information for gitlab account $gitlabAccount", e)
            } finally {
                gitlabApi?.close()
            }
        }
        return accountProjects
    }

    fun loadGitlabUser(gitlabAccount: GitlabAccount): GitlabUser {
        var gitlabApi: GitLabApi? = null
        try {
            gitlabApi = openGitlabApiAccess(gitlabAccount)
            val gitlabUser = gitlabApi.userApi.currentUser

            gitlabAccount.signedIn = true
            return GitlabUser(gitlabUser)
        } finally {
            gitlabApi?.close()
        }
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
        val gitlabPassword by lazy { authenticationManager.getAuthenticationPasswordFor(gitlabAccount) }

        val gitlabApi: GitLabApi
        when {
            gitlabAccessToken != null -> gitlabApi = GitLabApi(targetGitlabHost, gitlabAccessToken).apply {
                ignoreCertificateErrors = gitlabSettingsService.state.gitlabHostSettings[targetGitlabHost]?.disableSslVerification ?: false
            }
            gitlabPassword != null    -> gitlabApi =
                GitLabApi.oauth2Login(targetGitlabHost, gitlabAccount.username, gitlabPassword, gitlabSettingsService.state.gitlabHostSettings[targetGitlabHost]?.disableSslVerification ?: false)
            else                      -> throw IllegalArgumentException("Cannot access gitlab instance for host $targetGitlabHost with user $username. Missing access token to authenticate")
        }
        return gitlabApi
    }
}
