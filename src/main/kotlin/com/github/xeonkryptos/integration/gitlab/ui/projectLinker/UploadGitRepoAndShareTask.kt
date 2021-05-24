package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotificationIdsHolder
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.repo.GitRepository

@Suppress("DialogTitleCapitalization")
class UploadGitRepoAndShareTask(project: Project, projectLinkerConfiguration: ProjectLinkingConfiguration, private val gitRepository: GitRepository) :
    AbstractShareTask(project, projectLinkerConfiguration) {

    companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val git = service<Git>()

    override fun run(indicator: ProgressIndicator) {
        createGitlabRepository(indicator)

        indicator.checkCanceled()
        LOG.info("Adding Gitlab as a remote host")
        indicator.text = GitlabBundle.message("share.process.adding.gitlab.as.remote.host")
        git.addRemote(gitRepository, projectLinkingConfiguration.remoteName, remotePushUrl).throwOnError()
        gitRepository.update()

        indicator.checkCanceled()
        LOG.info("Pushing to Gitlab's main branch")
        indicator.text = GitlabBundle.message("share.process.pushing.to.gitlab.main")
        if (!pushCurrentBranch(gitRepository, remotePushUrl)) return

        GitlabNotifications.showInfoURL(project,
                                        GitlabNotificationIdsHolder.SHARE_PROJECT_SUCCESSFULLY_SHARED,
                                        GitlabBundle.message("share.process.successfully.shared"),
                                        projectLinkingConfiguration.projectName,
                                        createNewProjectUrl)
    }
}