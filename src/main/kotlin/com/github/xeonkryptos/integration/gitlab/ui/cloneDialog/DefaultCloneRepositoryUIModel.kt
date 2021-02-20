package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * @author Xeonkryptos
 * @since 20.02.2021
 */
class DefaultCloneRepositoryUIModel : CloneRepositoryUIModel {

    override val treeModel: DefaultTreeModel = DefaultTreeModel(DefaultMutableTreeNode())

    override fun reload() {
        treeModel.reload()
    }
}
