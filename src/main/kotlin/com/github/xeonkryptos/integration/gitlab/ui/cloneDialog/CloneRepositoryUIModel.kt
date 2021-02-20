package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import javax.swing.tree.TreeModel

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
interface CloneRepositoryUIModel {

    val treeModel: TreeModel

    fun reload()
}
