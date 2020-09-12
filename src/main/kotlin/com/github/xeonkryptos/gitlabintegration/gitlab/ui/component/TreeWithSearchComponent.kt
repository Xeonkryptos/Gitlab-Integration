package com.github.xeonkryptos.gitlabintegration.gitlab.ui.component

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.ui.treeStructure.SimpleTree
import java.util.function.Predicate
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel

/**
 * @author Xeonkryptos
 * @since 11.09.2020
 */
class TreeWithSearchComponent(originModel: TreeModel) {

    private val speedSearch: SpeedSearch = SpeedSearch(false)
    private val filterCondition: Predicate<Collection<String?>> = Predicate { userObjects ->
        val nodePath = userObjects.filterNotNull().joinToString("/")
        return@Predicate speedSearch.shouldBeShowing(nodePath)
    }

    private val filteringListModel: NameFilteringTreeModel<String> = NameFilteringTreeModel(originModel, { node -> (node as DefaultMutableTreeNode).userObject as? String }, filterCondition)

    val tree: SimpleTree = SimpleTree(filteringListModel).apply {
        isRootVisible = false
    }

    val searchField: SearchTextField = SearchTextField(false)

    init {
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = speedSearch.updatePattern(searchField.text)
        })

        // TODO: Selection handling necessary when re-filtering components?
        speedSearch.addChangeListener {
            //            val prevSelection = tree.selectedNode // save to restore the selection on filter drop
            filteringListModel.refilter()
            //            if (!filteringListModel.isEmpty()) {
            //                val fullMatchIndex = if (speedSearch.isHoldingFilter) filteringListModel.closestMatchIndex
            //                else filteringListModel.getElementIndex(prevSelection)
            //                if (fullMatchIndex != -1) {
            //                    tree.selectedIndex = fullMatchIndex
            //                }
            //
            //                if (filteringListModel.size <= tree.selectedIndex || !filteringListModel.contains(tree.selectedValue)) {
            //                    tree.selectedIndex = 0
            //                }
            //            }
        }
    }
}
