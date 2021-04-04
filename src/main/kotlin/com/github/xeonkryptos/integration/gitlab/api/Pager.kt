package com.github.xeonkryptos.integration.gitlab.api

import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.GenericType
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class Pager<T>(baseUri: URI, private val token: String, private val type: GenericType<T>, private val requestingClient: Client, private val entriesPerPage: Int = 50) : IPager<T> {

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
        val invocation = target.queryParam("per_page", entriesPerPage).request(MediaType.APPLICATION_JSON_TYPE).header("PRIVATE-TOKEN", token).buildGet()
        var response: Response? = null
        try {
            response = invocation.invoke()

            firstPageUri = response.getLink("first").uri
            previousPageUri = response.getLink("prev")?.uri
            nextPageUri = response.getLink("next")?.uri
            lastPageUri = response.getLink("last")?.uri

            return response.readEntity(type)
        } catch (e: Exception) {
            throw e
        } finally {
            response?.close()
        }
    }
}
