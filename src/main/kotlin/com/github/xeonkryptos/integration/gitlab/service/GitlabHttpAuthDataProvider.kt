package com.github.xeonkryptos.integration.gitlab.service

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.GitHttpAuthDataProvider

class GitlabHttpAuthDataProvider : GitHttpAuthDataProvider {

    private val authenticationManager = service<AuthenticationManager>()

    override fun getAuthData(project: Project, url: String): AuthData? {
        // TODO: An internal storage/mapping is required to retrieve for an HTTP URL a usable GitlabAccount instance. Such isn't defined yet
        return super.getAuthData(project, url)
    }
}