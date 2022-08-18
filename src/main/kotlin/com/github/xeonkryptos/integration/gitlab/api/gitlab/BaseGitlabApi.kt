package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.components.service
import jakarta.ws.rs.client.Client

sealed class BaseGitlabApi {

    val gitlabSettingsService = service<GitlabSettingsService>()
    val authenticationManager = service<AuthenticationManager>()

    protected fun getGitlabApiClient(gitlabAccount: GitlabAccount): Client {
        val targetGitlabHost = gitlabAccount.getTargetGitlabHost()
        return service<GitlabClient>().getGitlabApiClient(gitlabSettingsService.getWorkableState().gitlabHostSettings[targetGitlabHost])
    }
}
