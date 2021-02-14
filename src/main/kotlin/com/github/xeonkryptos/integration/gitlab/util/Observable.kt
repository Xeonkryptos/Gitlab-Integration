package com.github.xeonkryptos.integration.gitlab.util

import kotlin.reflect.KProperty

class Observable<T>(default: T) {

    private val listeners: MutableSet<((T, T) -> Unit)> = mutableSetOf()

    private var underlying = default
        set(value) {
            field = value
            listeners.forEach { it.invoke(field, value) }
        }

    fun get() = underlying

    fun addObserver(onChange: (old: T, new: T) -> Unit) = listeners.add(onChange)

    fun removeObserver(onChange: (old: T, new: T) -> Unit) = listeners.remove(onChange)

    operator fun getValue(thisRef: Any, prop: KProperty<*>) = underlying

    operator fun setValue(thisRef: Any, prop: KProperty<*>, value: T) {
        underlying = value
    }
}
