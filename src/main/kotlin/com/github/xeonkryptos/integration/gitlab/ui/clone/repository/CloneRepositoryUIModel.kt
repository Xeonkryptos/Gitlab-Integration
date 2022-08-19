package com.github.xeonkryptos.integration.gitlab.ui.clone.repository

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.ui.general.CollectionListModelExt
import java.util.*

class CloneRepositoryUIModel : CollectionListModelExt<GitlabProjectListItem>() {

    var availableAccounts: Collection<GitlabAccount> = Collections.emptyList()

    var hasPreviousRepositories: Boolean = false
    var hasNextRepositories: Boolean = false
}