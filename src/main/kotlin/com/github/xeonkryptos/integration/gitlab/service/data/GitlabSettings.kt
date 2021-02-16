package com.github.xeonkryptos.integration.gitlab.service.data

import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.XCollection

data class GitlabSettings(@XCollection(propertyElementName = "gitlabHosts") private var mutableGitlabHostSettings: MutableMap<String, GitlabHostSettings> = mutableMapOf()) {

    val gitlabHostSettings: Map<String, GitlabHostSettings>
        get() = mutableGitlabHostSettings

    fun onLoadingFinished(project: Project) {
        mutableGitlabHostSettings.values.forEach { it.onLoadingFinished(project) }
    }

    fun getOrCreateGitlabHostSettings(gitlabHost: String): GitlabHostSettings {
        return mutableGitlabHostSettings.computeIfAbsent(gitlabHost) { GitlabHostSettings(it) }
    }

    fun addGitlabAccountStateListener(listener: () -> Unit) {
        mutableGitlabHostSettings.values.forEach { it.addGitlabAccountStateListener(listener) }
    }

    fun getFirstGitlabAccountBy(filter: (GitlabAccount) -> Boolean): GitlabAccount? = mutableGitlabHostSettings.values.asSequence().flatMap { it.gitlabAccounts }.firstOrNull(filter)

    fun getAllGitlabAccountsBy(filter: (GitlabAccount) -> Boolean): List<GitlabAccount> = mutableGitlabHostSettings.values.asSequence().flatMap { it.gitlabAccounts }.filter(filter).toList()

    fun getAllGitlabAccounts(): List<GitlabAccount> = mutableGitlabHostSettings.values.asSequence().flatMap { it.gitlabAccounts }.toList()
}
