package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.xeonkryptos.integration.gitlab.api.Pager
import com.github.xeonkryptos.integration.gitlab.api.PagerProxy
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.AccessLevels
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabGroup
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabVisibility
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.GenericType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl

class GitlabGroupsApi(project: Project) : BaseGitlabApi(project) {

    companion object {
        private val GITLAB_GROUPS_GENERIC_TYPE: GenericType<List<GitlabGroup>> = GenericType<List<GitlabGroup>>(ParameterizedTypeImpl(List::class.java, GitlabGroup::class.java))
    }

    @RequiresBackgroundThread
    fun loadAvailableGroups(searchText: String? = null): MutableMap<GitlabAccount, PagerProxy<List<GitlabGroup>>> {
        val accountGroups: MutableMap<GitlabAccount, PagerProxy<List<GitlabGroup>>> = mutableMapOf()
        gitlabSettingsService.state.getAllGitlabAccounts().filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            loadAvailableGroupsFor(gitlabAccount, searchText)?.let { accountGroups[gitlabAccount] = it }
        }
        return accountGroups
    }

    @RequiresBackgroundThread
    fun loadAvailableGroupsFor(gitlabAccount: GitlabAccount, searchText: String? = null): PagerProxy<List<GitlabGroup>>? {
        if (authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
            var baseUriBuilder: UriBuilder = UriBuilder.fromUri(gitlabAccount.getTargetGitlabHost()).path("api/v4/groups").queryParam("owned", gitlabAccount.resolveOnlyOwnProjects)
                // Starting with the developer role, projects can be created. With a role lower than developer, the permission to create projects is missing
                .queryParam("min_access_level", AccessLevels.DEVELOPER.accessLevelId).queryParam("order_by", "full_name").queryParam("order_by", "id").queryParam("sort", "asc") // Default sort is desc
            if (searchText != null && searchText.isNotBlank()) {
                baseUriBuilder = baseUriBuilder.queryParam("search", searchText)
            }
            val baseUri = baseUriBuilder.build()
            val gitlabClient = getGitlabApiClient(gitlabAccount)
            val pager = Pager(baseUri, gitlabAccount, GITLAB_GROUPS_GENERIC_TYPE, gitlabClient)
            val pagerProxy = PagerProxy(pager)
            pagerProxy.loadFirstPage()
            return pagerProxy
        }
        return null
    }

    @RequiresBackgroundThread
    fun createNewGroup(groupName: String, parentId: Int? = null, description: String? = null, visibility: GitlabVisibility? = null, gitlabAccount: GitlabAccount): GitlabGroup? {
        if (authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
            val baseUri = UriBuilder.fromUri(gitlabAccount.getTargetGitlabHost()).path("api/v4/groups").build()
            val gitlabClient = getGitlabApiClient(gitlabAccount)
            val pathName = StringUtil.collapseWhiteSpace(groupName).lowercase().replace(Regex("\\s+"), "-")
            val enrichedRequestWithToken = authenticationManager.enrichRequestWithToken(gitlabClient.target(baseUri).request(), gitlabAccount)
            val response = enrichedRequestWithToken.post(Entity.json(CreateGroupRequest(groupName, pathName, parentId, visibility?.name?.lowercase(), description)))
            if (response.statusInfo == Response.Status.CREATED) {
                return response.readEntity(GitlabGroup::class.java)
            }
            val receivedErrorMessage = response.readEntity(String::class.java)
            throw UnexpectedResponseException(receivedErrorMessage)
        }
        return null
    }

    private data class CreateGroupRequest(@JsonProperty("name") val groupName: String,
                                          @JsonProperty("path") val path: String,
                                          @JsonProperty("parent_id") val parentId: Int?,
                                          @JsonProperty("visibility") val visibility: String?,
                                          @JsonProperty("description") val description: String?)
}