package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager


class ProjectLinkerAction : AnAction() {

    companion object {
        private val defaultProject: Project = ProjectManager.getInstance().defaultProject
    }

    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        // Sharing is technically possible if we have only 1 module or there is one module selected (out of a list of modules). Sharing of more than one module isn't supported.
        e.presentation.isEnabledAndVisible = if (project != null) {
            if (ModuleManager.getInstance(project).modules.size > 1) {
                val selectedModule: Module? = LangDataKeys.MODULE.getData(e.dataContext)
                selectedModule != null
            } else project != defaultProject
        } else false
    }

    override fun actionPerformed(e: AnActionEvent) {
        var selectedModule: Module? = LangDataKeys.MODULE.getData(e.dataContext)
        val project = e.project
        if (selectedModule == null && project != null) {
            selectedModule = ModuleManager.getInstance(project).modules[0]
        }
        if (selectedModule == null) {
            // TODO: Show notification to user. An inconsistent/unexpected state detected. Can't share anything with a gitlab instance
            return
        }
        // TODO: Check if module to share is already shared with a known gitlab instance. If yes, then choose another dialog to ask the user: "It is already shared. Do you want to share with another gitlab instance?"
        val gitlabSettingsService = GitlabSettingsService.getInstance(project!!)
        val allGitlabAccounts = gitlabSettingsService.state.getAllGitlabAccounts()

        val projectLinkerDialog = ProjectLinkerDialog(project)
        projectLinkerDialog.fillWithDefaultValues(selectedModule.name, allGitlabAccounts)
        if (projectLinkerDialog.showAndGet()) {
            // TODO: Extract chosen values from dialog and use them to share project
        }
    }
}