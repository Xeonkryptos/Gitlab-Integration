package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.api.gitlab.GitlabProjectsApi
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotificationIdsHolder
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.repo.GitRepository

@Suppress("DialogTitleCapitalization")
abstract class AbstractShareTask(project: Project, protected val projectLinkingConfiguration: ProjectLinkingConfiguration) : Task.Backgroundable(project, GitlabBundle.message("share.process")) {

    companion object {
        private val LOG = GitlabUtil.LOG
    }

    protected val createNewProjectUrl = GitlabProjectsApi.createNewProjectUrl(projectLinkingConfiguration.gitlabAccount).toString()

    protected lateinit var remotePushUrl: String
    protected lateinit var createdProject: GitlabProject

    protected fun createGitlabRepository(indicator: ProgressIndicator) {
        LOG.info("Creating Gitlab repository")
        indicator.text = GitlabBundle.message("share.process.creating.repository")
        createdProject = service<GitlabProjectsApi>().createNewProject(projectLinkingConfiguration.projectName,
                                                                       projectLinkingConfiguration.visibility,
                                                                       projectLinkingConfiguration.projectNamespaceId,
                                                                       projectLinkingConfiguration.description,
                                                                       projectLinkingConfiguration.gitlabAccount)
        remotePushUrl = if (projectLinkingConfiguration.gitlabAccount.useSSH) createdProject.sshUrlToRepo else createdProject.httpUrlToRepo
        LOG.info("Successfully created Gitlab repository")
    }

    protected fun pushCurrentBranch(repository: GitRepository, remoteUrl: String): Boolean {
        val currentBranch = repository.currentBranch
        if (currentBranch == null) {
            GitlabNotifications.showErrorURL(project,
                                             GitlabNotificationIdsHolder.SHARE_PROJECT_INIT_PUSH_FAILED,
                                             GitlabBundle.message("share.error.cannot.finish"),
                                             GitlabBundle.message("share.error.created.project"),
                                             " '${projectLinkingConfiguration.projectName}' ",
                                             GitlabBundle.message("share.error.push.no.current.branch"),
                                             createNewProjectUrl)
            return false
        }
        val result = service<Git>().push(repository, projectLinkingConfiguration.remoteName, remoteUrl, currentBranch.name, true)
        if (!result.success()) {
            GitlabNotifications.showErrorURL(project,
                                             GitlabNotificationIdsHolder.SHARE_PROJECT_INIT_PUSH_FAILED,
                                             GitlabBundle.message("share.error.cannot.finish"),
                                             GitlabBundle.message("share.error.created.project"),
                                             " '${projectLinkingConfiguration.projectName}' ",
                                             GitlabBundle.message("share.error.push.failed", result.errorOutputAsHtmlString),
                                             createNewProjectUrl)
            return false
        }
        return true
    }

    override fun onThrowable(error: Throwable) {
        GitlabNotifications.showError(project, GitlabNotificationIdsHolder.SHARE_CANNOT_CREATE_REPO, GitlabBundle.message("share.error.failed.to.create.repo"), error)
    }
}