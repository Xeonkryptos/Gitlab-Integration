package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import java.util.*

class DefaultCloneRepositoryUIModel : CloneRepositoryUIModel, CollectionListModelExt<GitlabProjectListItem>() {

    @Volatile
    override var availableAccounts: Collection<GitlabAccount> = Collections.emptyList()

    @Volatile
    override var hasPreviousRepositories: Boolean = false
    @Volatile
    override var hasNextRepositories: Boolean = false
}