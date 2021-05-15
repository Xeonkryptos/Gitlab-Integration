package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import javax.swing.ListModel

interface CloneRepositoryUIModel : ListModel<GitlabProjectListItem> {

    var availableAccounts: Collection<GitlabAccount>
}