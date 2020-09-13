package com.github.xeonkryptos.integration.gitlab.util

import java.util.function.Consumer
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode

/**
 * @author Xeonkryptos
 * @since 13.09.2020
 */
class TreeTraverseUtil private constructor() {

    companion object {

        @JvmStatic
        fun traverseTree(parent: TreeNode, model: TreeModel, actionOnLeafNode: Consumer<TreeNode>? = null, actionAfterTraversedChild: Consumer<TreeNode>? = null) {
            if (model.isLeaf(parent)) {
                actionOnLeafNode?.accept(parent)
            } else {
                val childCount = model.getChildCount(parent)
                for (i in 0 until childCount) {
                    val child = model.getChild(parent, i) as TreeNode
                    traverseTree(child, model, actionOnLeafNode, actionAfterTraversedChild)
                    actionAfterTraversedChild?.accept(child)
                }
            }
        }
    }
}
