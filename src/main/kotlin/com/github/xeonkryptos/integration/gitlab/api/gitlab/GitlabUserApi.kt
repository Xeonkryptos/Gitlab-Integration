package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class GitlabUserApi(project: Project) : BaseGitlabApi(project) {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    fun retrieveGitlabUsersFor(gitlabAccounts: Collection<GitlabAccount>): Map<GitlabAccount, GitlabUser> {
        val users = mutableMapOf<GitlabAccount, GitlabUser>()
        gitlabAccounts.filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            try {
                val gitlabClient = getGitlabApiClient(gitlabAccount)
                val gitlabAccessToken = getToken(gitlabAccount)
                val invocation = gitlabClient.target(gitlabAccount.getGitlabHost()).path("api/v4/user").request().header("PRIVATE-TOKEN", gitlabAccessToken).buildGet()
                users[gitlabAccount] = invocation.invoke(GitlabUser::class.java)
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve user information for gitlab account $gitlabAccount", e)
            }
        }
        return users
    }

    fun loadGitlabUser(gitlabHostSettings: GitlabHostSettings, accessToken: String): GitlabUser {
        val gitlabClient = GitlabClient.getGitlabApiClient(gitlabHostSettings)
        val invocation = gitlabClient.target(gitlabHostSettings.gitlabHost).path("api/v4/user").request().header("PRIVATE-TOKEN", accessToken).buildGet()
        return invocation.invoke(GitlabUser::class.java)
    }
}
