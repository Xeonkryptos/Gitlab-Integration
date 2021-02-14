package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import java.util.stream.Collectors
import org.gitlab4j.api.GitLabApi

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class GitlabApiManager(project: Project, private val dataService: GitlabDataService) {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val authenticationManager = AuthenticationManager.getInstance(project)

    fun retrieveGitlabUsersFor(gitlabAccounts: Collection<GitlabAccount>): Map<GitlabAccount, GitlabUser> {
        val users = mutableMapOf<GitlabAccount, GitlabUser>()
        gitlabAccounts.filter { it.signedIn }.forEach { gitlabAccount ->
            var gitlabApi: GitLabApi? = null
            try {
                gitlabApi = openGitlabApiAccess(gitlabAccount)
                users[gitlabAccount] = GitlabUser(gitlabApi.userApi.currentUser, gitlabAccount)
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

    private fun openGitlabApiAccess(gitlabAccount: GitlabAccount? = dataService.state.activeGitlabAccount): GitLabApi {
        if (gitlabAccount == null) {
            throw IllegalArgumentException("Cannot access a gitlab instance without account information")
        }
        val gitlabHost = gitlabAccount.gitlabHost
        val username = gitlabAccount.username
        if (gitlabHost == null || username == null) {
            throw IllegalStateException("Missing gitlab host or username. Unable to authenticate. Found host: $gitlabHost, found username: $username")
        }

        val targetGitlabHost = if (gitlabHost.endsWith("/")) gitlabHost.substring(0, gitlabHost.length - 1); else gitlabHost
        val gitlabAccessToken = authenticationManager.getAuthenticationTokenFor(gitlabAccount) ?: throw IllegalArgumentException(
            "Cannot access gitlab instance for host $targetGitlabHost with user $username. Missing access token to authenticate"
                                                                                                                                )
        return GitLabApi(targetGitlabHost, gitlabAccessToken)
    }

    fun loadGitlabUser(host: String, accessToken: String): GitlabUser {
        var gitlabApi: GitLabApi? = null
        try {
            gitlabApi = GitLabApi(host, accessToken)
            val gitlabUser = gitlabApi.userApi.currentUser
            val gitlabAccount = GitlabAccount(host, gitlabUser.username)
            gitlabAccount.signedIn = true
            return GitlabUser(gitlabUser, gitlabAccount)
        } finally {
            gitlabApi?.close()
        }
    }
}
