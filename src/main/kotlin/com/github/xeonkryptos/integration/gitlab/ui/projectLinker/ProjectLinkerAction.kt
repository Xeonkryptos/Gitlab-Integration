package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.DumbAwareAction
import git4idea.repo.GitRepositoryManager

class ProjectLinkerAction : DumbAwareAction(GitlabBundle.message("share.action"), GitlabBundle.message("share.action.description"), GitlabUtil.GITLAB_ICON) {

    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabledAndVisible = project != null && !project.isDefault
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null || project.isDisposed) return
        service<FileDocumentManager>().saveAllDocuments()

        val gitlabSettingsService = service<GitlabSettingsService>()
        val allGitlabAccounts = gitlabSettingsService.state.getAllGitlabAccounts()

        val module: Module? = if (file != null) ModuleUtil.findModuleForFile(file, project) else null
        val projectLinkerDialog = ProjectLinkerDialog(project, module)

        projectLinkerDialog.fillWithDefaultValues(allGitlabAccounts)
        if (projectLinkerDialog.showAndGet()) {
            val gitlabHostWithoutProtocol = "${projectLinkerDialog.selectedAccount!!.getGitlabDomain()}/"
            val gitRepositoryManager = project.service<GitRepositoryManager>()
            val foundGitRepository = gitRepositoryManager.getRepositoryForRootQuick(projectLinkerDialog.rootDirVirtualFile)
            val projectLinkerConfiguration = projectLinkerDialog.constructProjectLinkerConfiguration()
            if (foundGitRepository != null && foundGitRepository.remotes.asSequence().flatMap { it.pushUrls }.any { it.contains(gitlabHostWithoutProtocol) }) {
                // TODO: Exists already on the gitlab. Ask, if it should be uploaded again.
            } else if (foundGitRepository != null) {
                UploadGitRepoAndShareTask(project, projectLinkerConfiguration, foundGitRepository).queue()
            } else {
                CreateNewGitRepoAndShareTask(project, projectLinkerConfiguration).queue()
            }
        }
    }
}