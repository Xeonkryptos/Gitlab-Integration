package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.xeonkryptos.integration.gitlab.api.Pager
import com.github.xeonkryptos.integration.gitlab.api.PagerProxy
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabVisibility
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.GenericType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import java.net.URI
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl

class GitlabProjectsApi(project: Project) : BaseGitlabApi(project) {

    companion object {
        private val LOG = GitlabUtil.LOG

        private val GITLAB_PROJECTS_GENERIC_TYPE: GenericType<List<GitlabProject>> = GenericType<List<GitlabProject>>(ParameterizedTypeImpl(List::class.java, GitlabProject::class.java))

        const val PROJECTS_API_PATH: String = "api/v4/projects"

        fun createNewProjectUrl(gitlabAccount: GitlabAccount): URI = UriBuilder.fromUri(gitlabAccount.getTargetGitlabHost()).path(PROJECTS_API_PATH).build()
    }

    fun retrieveGitlabProjectsFor(gitlabAccounts: Collection<GitlabAccount>, searchText: String? = null): Map<GitlabAccount, PagerProxy<List<GitlabProject>>> {
        val accountProjects = mutableMapOf<GitlabAccount, PagerProxy<List<GitlabProject>>>()
        gitlabAccounts.filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            try {
                var baseUriBuilder: UriBuilder = UriBuilder.fromUri(gitlabAccount.getTargetGitlabHost())
                    .path(PROJECTS_API_PATH)
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
                val gitlabClient = getGitlabApiClient(gitlabAccount)
                val pager = Pager(baseUri, gitlabAccount, GITLAB_PROJECTS_GENERIC_TYPE, gitlabClient, enrichmentAction = { it.forEach { project -> project.gitlabAccount = gitlabAccount } })
                val pagerProxy = PagerProxy(pager)
                accountProjects[gitlabAccount] = pagerProxy
                pagerProxy.loadFirstPage()
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve project information for gitlab account $gitlabAccount", e)
            }
        }
        return accountProjects
    }

    fun createNewProject(projectName: String,
                         visibility: GitlabVisibility? = GitlabVisibility.PRIVATE,
                         namespaceId: Long? = null,
                         description: String? = null,
                         gitlabAccount: GitlabAccount): GitlabProject? {
        val baseUri = createNewProjectUrl(gitlabAccount)
        val gitlabClient = getGitlabApiClient(gitlabAccount)
        val enrichedRequestWithToken = authenticationManager.enrichRequestWithToken(gitlabClient.target(baseUri).request(), gitlabAccount)
        val response = enrichedRequestWithToken.post(Entity.json(CreateProjectRequest(projectName, visibility?.name?.lowercase(), namespaceId, description)))
        if (response.statusInfo == Response.Status.CREATED) {
            return response.readEntity(GitlabProject::class.java)!!
        }
        val receivedErrorMessage = response.readEntity(String::class.java)
        throw UnexpectedResponseException(receivedErrorMessage)
    }

    private data class CreateProjectRequest(@JsonProperty("name") val projectName: String,
                                            @JsonProperty("visibility") val visibility: String? = null,
                                            @JsonProperty("namespace_id") val namespaceId: Long? = null,
                                            @JsonProperty("description") val description: String? = null)
}