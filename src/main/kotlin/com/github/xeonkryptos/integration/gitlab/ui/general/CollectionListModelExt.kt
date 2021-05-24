package com.github.xeonkryptos.integration.gitlab.ui.general

import com.intellij.ui.CollectionListModel
import java.util.function.Predicate

open class CollectionListModelExt<T> : CollectionListModel<T>() {

    fun removeIf(filter: Predicate<T>) = internalList.removeIf(filter)
}