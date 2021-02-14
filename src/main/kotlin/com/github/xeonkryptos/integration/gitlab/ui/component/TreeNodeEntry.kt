package com.github.xeonkryptos.integration.gitlab.ui.component

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject

class TreeNodeEntry(val pathName: String, var gitlabProject: GitlabProject? = null) {

    override fun toString() = pathName
}
