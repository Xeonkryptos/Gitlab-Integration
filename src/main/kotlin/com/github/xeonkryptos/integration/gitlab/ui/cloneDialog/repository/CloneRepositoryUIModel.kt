package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import java.util.function.Predicate
import javax.swing.ListModel

interface CloneRepositoryUIModel : ListModel<GitlabProjectListItem> {

    var availableAccounts: Collection<GitlabAccount>
    var hasPreviousRepositories: Boolean
    var hasNextRepositories: Boolean

    fun add(item: List<GitlabProjectListItem>)

    fun removeAll()

    fun removeIf(filter: Predicate<GitlabProjectListItem>): Boolean
}