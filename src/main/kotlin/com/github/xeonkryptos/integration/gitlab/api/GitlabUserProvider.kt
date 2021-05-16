package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
class GitlabUserProvider(private val gitlabUserApi: GitlabUserApi, private val gitlabSettings: GitlabSettings) : UserProvider {

    override fun getUsers(): Map<GitlabAccount, GitlabUser> {
        return gitlabUserApi.retrieveGitlabUsersFor(gitlabSettings.getAllGitlabAccounts())
    }
}
