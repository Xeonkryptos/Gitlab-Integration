package com.github.xeonkryptos.integration.gitlab.api

interface IPager<T> {

    fun loadFirstPage(): T

    fun loadPreviousPage(): T?

    fun hasPreviousPage(): Boolean

    fun loadNextPage(): T?

    fun hasNextPage(): Boolean

    fun loadLast(): T?

    fun canLoadLast(): Boolean
}
