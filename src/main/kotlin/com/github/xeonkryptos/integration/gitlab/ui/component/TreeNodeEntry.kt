package com.github.xeonkryptos.integration.gitlab.ui.component

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProjectWrapper

class TreeNodeEntry(val pathName: String, var gitlabProject: GitlabProjectWrapper? = null) {

    override fun toString() = pathName
}
