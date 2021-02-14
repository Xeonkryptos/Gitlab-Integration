package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.service.GitlabDataService

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
class GitlabUserProvider(private val gitlabApiManager: GitlabApiManager, private val gitlabDataService: GitlabDataService) : UserProvider {

    override fun getUsers() = gitlabApiManager.retrieveGitlabUsersFor(gitlabDataService.state.gitlabAccounts).values.toList()
}
