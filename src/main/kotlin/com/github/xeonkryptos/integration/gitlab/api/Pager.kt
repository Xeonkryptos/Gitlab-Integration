package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.components.service
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.GenericType
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.net.URI

class Pager<T>(baseUri: URI,
               private val gitlabAccount: GitlabAccount,
               private val type: GenericType<T>,
               private val requestingClient: Client,
               private val entriesPerPage: Int = 50,
               private val enrichmentAction: ((T) -> Unit)? = null) : IPager<T> {

    companion object {
        private val authenticationManager: AuthenticationManager = service()
    }

    @Volatile
    private var firstPageUri: URI = baseUri
    @Volatile
    private var previousPageUri: URI? = null
    @Volatile
    private var nextPageUri: URI? = null
    @Volatile
    private var lastPageUri: URI? = null

    override fun loadFirstPage(): T {
        return loadPage(firstPageUri)
    }

    override fun loadPreviousPage(): T? {
        if (hasPreviousPage()) {
            return loadPage(previousPageUri!!)
        }
        return null
    }

    override fun hasPreviousPage(): Boolean = previousPageUri != null

    override fun loadNextPage(): T? {
        if (hasNextPage()) {
            return loadPage(nextPageUri!!)
        }
        return null
    }

    override fun hasNextPage(): Boolean = nextPageUri != null

    override fun loadLast(): T? {
        if (!canLoadLast()) {
            throw IllegalStateException("Cannot load last page. URL for last page is unknown. Either no initial load request got executed or there are too many records thus gitlab didn't calculated the last page URL")
        }
        return loadPage(lastPageUri!!)
    }

    override fun canLoadLast(): Boolean = lastPageUri != null

    private fun loadPage(uri: URI): T {
        val target = requestingClient.target(uri)
        return loadPage(target)
    }

    private fun loadPage(target: WebTarget): T {
        val invocation = authenticationManager.enrichRequestWithToken(target.queryParam("per_page", entriesPerPage).request(MediaType.APPLICATION_JSON_TYPE), gitlabAccount).buildGet()
        var response: Response? = null
        try {
            response = invocation.invoke()

            firstPageUri = response.getLink("first").uri
            previousPageUri = response.getLink("prev")?.uri
            nextPageUri = response.getLink("next")?.uri
            lastPageUri = response.getLink("last")?.uri

            val parsedEntity = response.readEntity(type)
            enrichmentAction?.invoke(parsedEntity)
            return parsedEntity
        } catch (e: Exception) {
            throw e
        } finally {
            response?.close()
        }
    }
}
