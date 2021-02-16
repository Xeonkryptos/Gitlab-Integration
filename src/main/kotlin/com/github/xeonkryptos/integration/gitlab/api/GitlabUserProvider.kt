package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
class GitlabUserProvider(private val gitlabApiManager: GitlabApiManager, private val gitlabSettings: GitlabSettings) : UserProvider {

    override fun getUsers(): Map<GitlabAccount, GitlabUser> {
        return gitlabApiManager.retrieveGitlabUsersFor(gitlabSettings.getAllGitlabAccounts())
    }
}
