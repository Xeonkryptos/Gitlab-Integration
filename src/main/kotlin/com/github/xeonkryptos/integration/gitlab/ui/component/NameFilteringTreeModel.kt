package com.github.xeonkryptos.integration.gitlab.ui.component

import com.github.xeonkryptos.integration.gitlab.util.TreeTraverseUtil
import com.intellij.ui.tree.TreePathUtil
import java.util.function.Function
import java.util.function.Predicate
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode

/**
 * @author Xeonkryptos
 * @since 11.09.2020
 */
class NameFilteringTreeModel<T>(private val originModel: TreeModel,
                                private val treeNodeContentExtractor: Function<Any?, T?>,
                                private val filterCondition: Predicate<List<T?>>) : DefaultTreeModel(DefaultMutableTreeNode()) {

    private val listener = object : TreeModelListener {
        override fun treeNodesChanged(e: TreeModelEvent?) {
            refilter()
        }

        override fun treeNodesInserted(e: TreeModelEvent?) {
            refilter()
        }

        override fun treeNodesRemoved(e: TreeModelEvent?) {
            refilter()
        }

        override fun treeStructureChanged(e: TreeModelEvent?) {
            refilter()
        }
    }

    init {
        originModel.addTreeModelListener(listener)
        refilter()
    }

    fun refilter() {
        val originRootNode = originModel.root as TreeNode
        if (originRootNode.isLeaf) {
            (root as DefaultMutableTreeNode).removeAllChildren()
        } else {
            TreeTraverseUtil.traverseTree(originRootNode, originModel, { filterLeafNodesStartingAt(it) }) { treeNode ->
                val treePath = TreePathUtil.pathToTreeNode(treeNode)
                val convertedNodeElements = treePath.path.map { node -> treeNodeContentExtractor.apply(node) }
                if (treeNode.isLeaf.and(filterCondition.test(convertedNodeElements).not())) {
                    removeNode(treeNode, root as DefaultMutableTreeNode)
                }
            }
        }
        nodeStructureChanged(root)
    }

    private fun filterLeafNodesStartingAt(leafNodeOrigin: TreeNode) {
        val treePath = TreePathUtil.pathToTreeNode(leafNodeOrigin)
        val extractedNodeContents = treePath.path.mapNotNull { node -> treeNodeContentExtractor.apply(node) }

        var currentParent = root as DefaultMutableTreeNode
        if (filterCondition.test(extractedNodeContents)) {
            extractedNodeContents.forEach { extractedNodeContent ->
                val matchingNode = findMatchingNode(extractedNodeContent, currentParent)

                currentParent = when (matchingNode.first) {
                    MatchingNode.IDENTICAL -> matchingNode.second!!
                    MatchingNode.SAME_PATH_MATCHING -> {
                        removeNode(leafNodeOrigin, currentParent)
                        val newNode = DefaultMutableTreeNode(extractedNodeContent)
                        currentParent.add(newNode)
                        newNode
                    }
                    MatchingNode.NOT_FOUND -> {
                        val newNode = DefaultMutableTreeNode(extractedNodeContent)
                        currentParent.add(newNode)
                        newNode
                    }
                }
            }
        } else if (extractedNodeContents.isNotEmpty()) {
            removeNode(leafNodeOrigin, currentParent)
        }
    }

    private fun removeNode(nodeOrigin: TreeNode, startNode: DefaultMutableTreeNode) {
        val treePath = TreePathUtil.pathToTreeNode(nodeOrigin)
        val extractedNodeContents = treePath.path.mapNotNull { node -> treeNodeContentExtractor.apply(node) }
        var currentParent = startNode
        extractedNodeContents.forEach { extractedNodeContent ->
            if (currentParent.isLeaf) {
                return@forEach
            }
            val matchingNode = findMatchingNode(extractedNodeContent, currentParent)
            if (matchingNode.first == MatchingNode.SAME_PATH_MATCHING || matchingNode.first == MatchingNode.IDENTICAL) {
                currentParent = matchingNode.second!!
            } else {
                return
            }
        }
        currentParent.removeFromParent()
    }

    private fun findMatchingNode(convertedNodeElement: Any, parent: DefaultMutableTreeNode): Pair<MatchingNode, DefaultMutableTreeNode?> {
        for (i in 0 until parent.childCount) {
            val currentChild = parent.getChildAt(i) as DefaultMutableTreeNode
            if (currentChild.userObject == convertedNodeElement) {
                val isIdentical: Boolean = (currentChild.userObject as TreeNodeEntry).gitlabProject == (convertedNodeElement as TreeNodeEntry).gitlabProject
                return if (isIdentical) Pair(MatchingNode.IDENTICAL, currentChild) else Pair(MatchingNode.SAME_PATH_MATCHING, currentChild)
            }
        }
        return Pair(MatchingNode.NOT_FOUND, null)
    }

    fun isNotEmpty() = !isEmpty()

    fun isEmpty() = root.isLeaf

    private enum class MatchingNode {

        NOT_FOUND, SAME_PATH_MATCHING, IDENTICAL
    }
}
