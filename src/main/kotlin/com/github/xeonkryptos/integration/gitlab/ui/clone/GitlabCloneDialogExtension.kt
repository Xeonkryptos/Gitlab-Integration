package com.github.xeonkryptos.integration.gitlab.ui.clone

import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import javax.swing.Icon

/**
 * @author Xeonkryptos
 * @since 06.09.2020
 */
class GitlabCloneDialogExtension : VcsCloneDialogExtension {

    @Suppress("OverridingDeprecatedMember")
    @Deprecated("Implement createMainComponent(Project, ModalityState)")
    override fun createMainComponent(project: Project): VcsCloneDialogExtensionComponent {
        throw AssertionError("Shouldn't be called") // NON-NLS
    }

    override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent = GitlabCloneDialogExtensionComponent(project)

    override fun getIcon(): Icon = GitlabUtil.GITLAB_ICON

    override fun getName(): String = "Gitlab"
}
