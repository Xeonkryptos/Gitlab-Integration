package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import git4idea.repo.GitRepositoryManager
import java.nio.file.Path


class ProjectLinkerAction : AnAction() {

    companion object {
        private val defaultProject: Project = service<ProjectManager>().defaultProject
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
        val project = e.getData(CommonDataKeys.PROJECT)
        // Sharing is technically possible if we have only 1 module or there is one module selected (out of a list of modules). Sharing of more than one module isn't supported.
        e.presentation.isEnabled = if (project != null) {
            if (ModuleManager.getInstance(project).modules.size > 1) LangDataKeys.MODULE.getData(e.dataContext) != null else project != defaultProject
        } else false
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.presentation.isVisible = true
        var selectedModule: Module? = LangDataKeys.MODULE.getData(e.dataContext)
        var project = e.project
        if (selectedModule == null && project != null) {
            selectedModule = ModuleManager.getInstance(project).modules[0]
        }
        if (selectedModule == null) {
            e.presentation.isEnabled = false
            return
        }
        if (project == null) {
            project = selectedModule.project
        }
        val gitlabSettingsService = project.service<GitlabSettingsService>()
        val allGitlabAccounts = gitlabSettingsService.state.getAllGitlabAccounts()

        service<FileDocumentManager>().saveAllDocuments()

        val projectLinkerDialog = ProjectLinkerDialog(selectedModule, project)
        projectLinkerDialog.fillWithDefaultValues(allGitlabAccounts)
        if (projectLinkerDialog.showAndGet()) {
            val gitlabHost = projectLinkerDialog.selectedAccount!!.getTargetGitlabHost()
            val gitlabHostWithoutProtocol = "${gitlabHost.replace(Regex("http?://"), "")}/"

            val gitRepositoryManager = GitRepositoryManager.getInstance(project)
            val moduleRootDirVirtualFile = VfsUtil.findFile(Path.of(projectLinkerDialog.moduleRootDir), false)!!
            val foundGitRepository = gitRepositoryManager.getRepositoryForRootQuick(moduleRootDirVirtualFile)
            if (foundGitRepository != null && foundGitRepository.remotes.asSequence().flatMap { it.pushUrls }.any { it.contains(gitlabHostWithoutProtocol) }) {
                // TODO: Exists already on the gitlab. Ask, if it should be uploaded again.
            } else if (foundGitRepository != null) {
                // TODO: Project isn't at the configured gitlab instance (at least, not yet after looking into configured/known remotes), but a git repository is available. So, simply upload it and add
                //  it as a new remote.
            } else {
                // TODO: Isn't a git repository. Create a git repository and upload it to gitlab after creating the project
            }
        }
    }
}