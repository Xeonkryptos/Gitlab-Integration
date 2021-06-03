package com.github.xeonkryptos.integration.gitlab.service

import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import git4idea.remote.GitRepositoryHostingService
import git4idea.remote.InteractiveGitHttpAuthDataProvider

// TODO: Implement
class GitlabRepositoryHostingService : GitRepositoryHostingService() {

    override fun getInteractiveAuthDataProvider(project: Project, url: String): InteractiveGitHttpAuthDataProvider? {
        return super.getInteractiveAuthDataProvider(project, url)
    }

    override fun getInteractiveAuthDataProvider(project: Project, url: String, login: String): InteractiveGitHttpAuthDataProvider? {
        return super.getInteractiveAuthDataProvider(project, url, login)
    }

    override fun getServiceDisplayName(): String = GitlabUtil.SERVICE_DISPLAY_NAME
}