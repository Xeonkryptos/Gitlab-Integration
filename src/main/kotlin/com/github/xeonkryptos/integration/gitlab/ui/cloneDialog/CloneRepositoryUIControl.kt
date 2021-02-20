package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
interface CloneRepositoryUIControl {

    var ui: CloneRepositoryUI?

    fun updateProjectName(projectName: String?)

    fun reloadData()
}
