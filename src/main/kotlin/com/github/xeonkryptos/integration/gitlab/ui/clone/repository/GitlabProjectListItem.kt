package com.github.xeonkryptos.integration.gitlab.ui.clone.repository

import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.util.ui.cloneDialog.SearchableListItem
import java.util.*

data class GitlabProjectListItem(val gitlabAccount: GitlabAccount, val gitlabProject: GitlabProject) : SearchableListItem, Comparable<GitlabProjectListItem> {

    override val stringToSearch: String
        get() = gitlabProject.viewableProjectPath

    fun customizeRenderer(renderer: ColoredListCellRenderer<GitlabProjectListItem>) = with(renderer) {
        ipad.left = 10
        toolTipText = gitlabProject.description
        append(gitlabProject.viewableProjectPath)
    }

    override fun compareTo(other: GitlabProjectListItem): Int {
        if (gitlabAccount == other.gitlabAccount && gitlabProject == other.gitlabProject) {
            return 0;
        }
        return gitlabProject.viewableProjectPath.compareTo(other.gitlabProject.viewableProjectPath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GitlabProjectListItem

        if (gitlabAccount != other.gitlabAccount) return false
        if (gitlabProject != other.gitlabProject) return false
        return true
    }

    override fun hashCode(): Int = Objects.hash(gitlabAccount, gitlabProject)

    override fun toString(): String {
        return gitlabProject.viewableProjectPath
    }
}