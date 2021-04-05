package com.github.xeonkryptos.integration.gitlab.ui.component

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject

class TreeNodeEntry(val pathName: String, var gitlabProject: GitlabProject? = null) {

    override fun toString() = pathName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TreeNodeEntry) return false

        return pathName == other.pathName
    }

    override fun hashCode(): Int {
        return pathName.hashCode()
    }
}
