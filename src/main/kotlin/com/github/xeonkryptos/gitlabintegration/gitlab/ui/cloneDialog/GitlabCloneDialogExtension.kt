package com.github.xeonkryptos.gitlabintegration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.gitlabintegration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import javax.swing.Icon

/**
 * @author Xeonkryptos
 * @since 06.09.2020
 */
class GitlabCloneDialogExtension : VcsCloneDialogExtension {

    override fun createMainComponent(project: Project): VcsCloneDialogExtensionComponent = GitlabCloneDialogExtensionComponent(project)

    override fun getIcon(): Icon = GitlabUtil.GITLAB_ICON

    override fun getName(): String = "Gitlab"
}
