package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.github.xeonkryptos.integration.gitlab.api.Pager
import com.github.xeonkryptos.integration.gitlab.api.PagerProxy
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.AccessLevels
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabGroup
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.project.Project
import jakarta.ws.rs.core.GenericType
import jakarta.ws.rs.core.UriBuilder
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl

class GitlabGroupsApi(project: Project) : BaseGitlabApi(project) {

    companion object {
        private val GITLAB_GROUPS_GENERIC_TYPE: GenericType<List<GitlabGroup>> = GenericType<List<GitlabGroup>>(ParameterizedTypeImpl(List::class.java, GitlabGroup::class.java))
    }

    fun loadAvailableGroups(searchText: String? = null): MutableMap<GitlabAccount, PagerProxy<List<GitlabGroup>>> {
        val accountGroups: MutableMap<GitlabAccount, PagerProxy<List<GitlabGroup>>> = mutableMapOf()
        gitlabSettingsService.state.getAllGitlabAccounts().filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            loadAvailableGroupsFor(gitlabAccount, searchText)?.let { accountGroups[gitlabAccount] = it}
        }
        return accountGroups
    }

    fun loadAvailableGroupsFor(gitlabAccount: GitlabAccount, searchText: String? = null): PagerProxy<List<GitlabGroup>>? {
        if (authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
            val gitlabClient = getGitlabApiClient(gitlabAccount)
            val token = getToken(gitlabAccount)
            var baseUriBuilder: UriBuilder = UriBuilder.fromUri(gitlabAccount.getTargetGitlabHost()).path("api/v4/groups").queryParam("owned", gitlabAccount.resolveOnlyOwnProjects)
                // Starting with the developer role, projects can be created. With a role lower than developer, the permission to create projects is missing
                .queryParam("min_access_level", AccessLevels.DEVELOPER.accessLevelId).queryParam("order_by", "full_name").queryParam("order_by", "id").queryParam("sort", "asc") // Default sort is desc
            if (searchText != null && searchText.isNotBlank()) {
                baseUriBuilder = baseUriBuilder.queryParam("search", searchText)
            }
            val baseUri = baseUriBuilder.build()
            return token?.let {
                val pager = Pager(baseUri, it, GITLAB_GROUPS_GENERIC_TYPE, gitlabClient)
                val pagerProxy = PagerProxy(pager)
                pagerProxy.loadFirstPage()
                return@let pagerProxy
            }
        }
        return null
    }
}