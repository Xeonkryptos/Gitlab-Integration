package com.github.xeonkryptos.integration.gitlab.api

class PagerProxy<T>(private val pager: Pager<T>) : IPager<T> by pager {

    var currentData: T? = null
        private set

    override fun loadFirstPage(): T {
        currentData = pager.loadFirstPage()
        return currentData!!
    }

    override fun loadPreviousPage(): T? {
        currentData = pager.loadPreviousPage()
        return currentData
    }

    override fun loadNextPage(): T? {
        currentData = loadNextPage()
        return currentData
    }

    override fun loadLast(): T? {
        currentData = pager.loadLast()
        return currentData
    }
}
