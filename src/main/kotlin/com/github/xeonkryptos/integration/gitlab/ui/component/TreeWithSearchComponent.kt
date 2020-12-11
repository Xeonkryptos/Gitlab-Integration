package com.github.xeonkryptos.integration.gitlab.ui.component

import com.github.xeonkryptos.integration.gitlab.util.TreeTraverseUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.ui.tree.TreePathUtil
import com.intellij.ui.treeStructure.SimpleTree
import java.util.function.Predicate
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

/**
 * @author Xeonkryptos
 * @since 11.09.2020
 */
class TreeWithSearchComponent(originModel: TreeModel) {

    private val speedSearch: SpeedSearch = SpeedSearch(false)
    private val filterCondition: Predicate<List<TreeNodeEntry?>> = Predicate { treeNodeEntries ->
        val nodePath = treeNodeEntries.asSequence().filterNotNull().map { it.pathName }.joinToString("/")
        return@Predicate speedSearch.shouldBeShowing(nodePath)
    }
    private val filteringListModel: NameFilteringTreeModel<TreeNodeEntry> =
        NameFilteringTreeModel(originModel, { node -> (node as DefaultMutableTreeNode).userObject as? TreeNodeEntry }, filterCondition)

    val tree: SimpleTree = SimpleTree(filteringListModel).apply {
        isRootVisible = false
    }
    val searchField: SearchTextField = SearchTextField(false)

    init {
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = speedSearch.updatePattern(searchField.text)
        })

        speedSearch.addChangeListener {
            val prevSelection = tree.selectionPath // save to restore the selection on filter drop
            val expandedPaths = ArrayList<TreePath>()
            TreeTraverseUtil.traverseTree(tree.model.root as TreeNode, tree.model, actionAfterTraversedChild = { treeNode ->
                val treePath = TreePathUtil.toTreePath(treeNode)
                if (tree.isExpanded(treePath)) {
                    expandedPaths.add(treePath)
                }
            })
            filteringListModel.refilter()
            if (filteringListModel.isNotEmpty()) {
                tree.selectionModel.addSelectionPath(prevSelection)
                expandedPaths.forEach(tree::expandPath)
            }
        }
    }
}
