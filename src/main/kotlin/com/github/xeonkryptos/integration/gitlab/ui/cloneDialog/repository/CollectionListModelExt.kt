package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.intellij.ui.CollectionListModel
import java.util.function.Predicate

open class CollectionListModelExt<T> : CollectionListModel<T>() {

    fun removeIf(filter: Predicate<T>) = internalList.removeIf(filter)
}