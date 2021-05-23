package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.components.service

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class GitlabUserApi : BaseGitlabApi() {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    fun retrieveGitlabUsersFor(gitlabAccounts: Collection<GitlabAccount>): Map<GitlabAccount, GitlabUser> {
        val users = mutableMapOf<GitlabAccount, GitlabUser>()
        gitlabAccounts.filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            try {
                val gitlabClient = getGitlabApiClient(gitlabAccount)
                val baseRequest = gitlabClient.target(gitlabAccount.getGitlabHost()).path("api/v4/user").request()
                val invocation = authenticationManager.enrichRequestWithToken(baseRequest, gitlabAccount).buildGet()
                users[gitlabAccount] = invocation.invoke(GitlabUser::class.java)
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve user information for gitlab account $gitlabAccount", e)
            }
        }
        return users
    }

    fun loadGitlabUser(gitlabHostSettings: GitlabHostSettings, accessToken: String): GitlabUser {
        val gitlabClient = service<GitlabClient>().getGitlabApiClient(gitlabHostSettings)
        val invocation = gitlabClient.target(gitlabHostSettings.gitlabHost).path("api/v4/user").request().header("PRIVATE-TOKEN", accessToken).buildGet()
        return invocation.invoke(GitlabUser::class.java)
    }
}
