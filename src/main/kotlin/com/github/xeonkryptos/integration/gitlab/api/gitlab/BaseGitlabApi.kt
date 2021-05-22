package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import jakarta.ws.rs.client.Client

sealed class BaseGitlabApi(project: Project) {

    val gitlabSettingsService = project.service<GitlabSettingsService>()
    val authenticationManager = service<AuthenticationManager>()

    protected fun getGitlabApiClient(gitlabAccount: GitlabAccount): Client {
        val targetGitlabHost = gitlabAccount.getTargetGitlabHost()
        return service<GitlabClient>().getGitlabApiClient(gitlabSettingsService.state.gitlabHostSettings[targetGitlabHost])
    }

    protected fun getToken(gitlabAccount: GitlabAccount): String? {
        val username = gitlabAccount.username
        val gitlabAccessToken by lazy { authenticationManager.getAuthenticationTokenFor(gitlabAccount) }
        if (gitlabAccessToken != null) return gitlabAccessToken
        throw IllegalArgumentException("Cannot access gitlab instance for host ${gitlabAccount.getTargetGitlabHost()} with user $username. Missing access token to authenticate")
    }
}
