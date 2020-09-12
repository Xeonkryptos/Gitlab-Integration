package com.github.xeonkryptos.gitlabintegration.gitlab.ui.cloneDialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import javax.swing.Icon

/**
 * @author Xeonkryptos
 * @since 06.09.2020
 */
class GitlabCloneDialogExtension : VcsCloneDialogExtension {

    private companion object {
        private val GITLAB_ICON = IconLoader.getIcon("/icons/gitlab-icon-rgb.svg")
    }

    override fun createMainComponent(project: Project): VcsCloneDialogExtensionComponent = GitlabCloneDialogExtensionComponent(project)

    override fun getIcon(): Icon = GITLAB_ICON

    override fun getName(): String = "Gitlab"
}
