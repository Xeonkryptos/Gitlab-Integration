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
class NameFilteringTreeModel<T>(private val originModel: TreeModel, private val treeNodeContentExtractor: Function<Any?, T?>, private val filterCondition: Predicate<List<T?>>) : DefaultTreeModel(
    DefaultMutableTreeNode()) {

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
        TreeTraverseUtil.traverseTree(originModel.root as TreeNode, originModel, { filterLeafNodesStartingAt(it) }, { treeNode ->
            val treePath = TreePathUtil.pathToTreeNode(treeNode)
            val convertedNodeElements = treePath.path.map { node -> treeNodeContentExtractor.apply(node) }
            if (treeNode.isLeaf.and(filterCondition.test(convertedNodeElements).not())) {
                removeNode(treeNode, root as DefaultMutableTreeNode)
            }
        })
        nodeStructureChanged(root) // TODO: Some tree state restore mechanism required after this? Something like in TreeWithSearchComponent just moved to here to be globally used?
    }

    private fun filterLeafNodesStartingAt(leafNodeOrigin: TreeNode) {
        val treePath = TreePathUtil.pathToTreeNode(leafNodeOrigin)
        val extractedNodeContents = treePath.path.mapNotNull { node -> treeNodeContentExtractor.apply(node) }

        var currentParent = root as DefaultMutableTreeNode
        if (filterCondition.test(extractedNodeContents)) {
            extractedNodeContents.forEach { extractedNodeContent ->
                val matchingNode = findMatchingNode(extractedNodeContent, currentParent)
                currentParent = if (matchingNode == null) {
                    val newNode = DefaultMutableTreeNode(extractedNodeContent)
                    currentParent.add(newNode)
                    newNode
                } else {
                    matchingNode
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
            if (matchingNode != null) {
                currentParent = matchingNode
            } else {
                return
            }
        }
        currentParent.removeFromParent()
    }

    private fun findMatchingNode(convertedNodeElement: Any, parent: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        for (i in 0 until parent.childCount) {
            val currentChild = parent.getChildAt(i) as DefaultMutableTreeNode
            if (currentChild.userObject == convertedNodeElement) {
                return currentChild
            }
        }
        return null
    }

    fun isNotEmpty() = !isEmpty()

    fun isEmpty() = root.isLeaf
}