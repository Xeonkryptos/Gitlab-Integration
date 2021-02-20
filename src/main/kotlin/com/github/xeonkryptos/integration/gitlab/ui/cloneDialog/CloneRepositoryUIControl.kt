package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
interface CloneRepositoryUIControl {

    var ui: CloneRepositoryUI?

    fun updateProjectName(projectName: String?)

    fun reloadData(gitlabAccount: GitlabAccount? = null)
}
