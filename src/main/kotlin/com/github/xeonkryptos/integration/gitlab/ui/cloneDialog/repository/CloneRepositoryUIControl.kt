package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
interface CloneRepositoryUIControl {

    var ui: CloneRepositoryUI?

    fun updateProjectName(projectName: String?)

    fun reloadData(gitlabAccount: GitlabAccount? = null)

    fun hasPreviousRepositories(): Boolean

    fun loadPreviousRepositories()

    fun hasNextRepositories(): Boolean

    fun loadNextRepositories()

    fun getAvailableAccounts() : Collection<GitlabAccount>
}
