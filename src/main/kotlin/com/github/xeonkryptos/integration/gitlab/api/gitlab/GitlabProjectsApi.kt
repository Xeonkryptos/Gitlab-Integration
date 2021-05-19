package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.github.xeonkryptos.integration.gitlab.api.Pager
import com.github.xeonkryptos.integration.gitlab.api.PagerProxy
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import jakarta.ws.rs.core.GenericType
import jakarta.ws.rs.core.UriBuilder
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl

class GitlabProjectsApi(project: Project) : BaseGitlabApi(project) {

    private companion object {
        private val LOG = GitlabUtil.LOG

        private val GITLAB_PROJECTS_GENERIC_TYPE: GenericType<List<GitlabProject>> = GenericType<List<GitlabProject>>(ParameterizedTypeImpl(List::class.java, GitlabProject::class.java))
    }

    fun retrieveGitlabProjectsFor(gitlabAccounts: Collection<GitlabAccount>, searchText: String? = null): Map<GitlabAccount, PagerProxy<List<GitlabProject>>> {
        val accountProjects = mutableMapOf<GitlabAccount, PagerProxy<List<GitlabProject>>>()
        gitlabAccounts.filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            try {
                val gitlabClient = getGitlabApiClient(gitlabAccount)
                val token = getToken(gitlabAccount)
                var baseUriBuilder: UriBuilder = UriBuilder.fromUri(gitlabAccount.getTargetGitlabHost())
                    .path("api/v4/projects")
                    .queryParam("owned", gitlabAccount.resolveOnlyOwnProjects)
                    .queryParam("membership", gitlabAccount.resolveOnlyOwnProjects)
                    .queryParam("order_by", "path") // not the same as namespace (not every time, but namespace isn't a valid sort option...)
                    .queryParam("order_by", "name")
                    .queryParam("order_by", "id")
                    .queryParam("simple", true)
                    .queryParam("sort", "asc") // Default sort is desc

                if (searchText != null && searchText.isNotBlank()) {
                    baseUriBuilder = baseUriBuilder.queryParam("search", searchText)
                }
                val baseUri = baseUriBuilder.build()
                token?.let {
                    val pager = Pager(baseUri, it, GITLAB_PROJECTS_GENERIC_TYPE, gitlabClient)
                    val pagerProxy = PagerProxy(pager)
                    accountProjects[gitlabAccount] = pagerProxy
                    pagerProxy.loadFirstPage()
                }
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve project information for gitlab account $gitlabAccount", e)
            }
        }
        return accountProjects
    }


}