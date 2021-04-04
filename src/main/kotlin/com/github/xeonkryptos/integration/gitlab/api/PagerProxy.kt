package com.github.xeonkryptos.integration.gitlab.api

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class PagerProxy<T>(private val pager: Pager<T>) : IPager<T> by pager {

    private val counter: AtomicInteger = AtomicInteger(0)

    @Volatile
    var currentData: T? = null
        private set

    override fun loadFirstPage(): T {
        counter.set(0)
        currentData = pager.loadFirstPage()
        return currentData!!
    }

    override fun loadPreviousPage(): T? {
        val count = counter.updateAndGet { max(it - 1, 0) }
        if (count == 0) {
            currentData = pager.loadPreviousPage()
            return currentData
        }
        return null
    }

    override fun loadNextPage(): T? {
        currentData = pager.loadNextPage()
        if (currentData == null) {
            counter.incrementAndGet()
        }
        return currentData
    }

    override fun loadLast(): T? {
        currentData = pager.loadLast()
        return currentData
    }
}
