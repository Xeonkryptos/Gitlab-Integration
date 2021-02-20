package com.github.xeonkryptos.integration.gitlab.service.data

import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.rd.util.concurrentMapOf

data class GitlabSettings(@Volatile @XCollection(propertyElementName = "gitlabHosts") private var mutableGitlabHostSettings: MutableMap<String, GitlabHostSettings> = concurrentMapOf()) {

    val gitlabHostSettings: Map<String, GitlabHostSettings> = mutableGitlabHostSettings

    fun onLoadingFinished(project: Project) {
        mutableGitlabHostSettings.values.forEach { it.onLoadingFinished(project) }
    }

    fun getOrCreateGitlabHostSettings(gitlabHost: String): GitlabHostSettings {
        return mutableGitlabHostSettings.computeIfAbsent(gitlabHost) { GitlabHostSettings(it) }
    }

    fun isModified(gitlabSettings: GitlabSettings): Boolean {
        if (this != gitlabSettings) return true
        return mutableGitlabHostSettings.any { entry -> entry.value.isModified(gitlabSettings.mutableGitlabHostSettings[entry.key]!!) }
    }

    fun hasGitlabAccountBy(filter: (GitlabAccount) -> Boolean): Boolean = getFirstGitlabAccountBy(filter) != null

    fun getFirstGitlabAccountBy(filter: (GitlabAccount) -> Boolean): GitlabAccount? = mutableGitlabHostSettings.values.asSequence().flatMap { it.gitlabAccounts }.firstOrNull(filter)

    fun getAllGitlabAccountsBy(filter: (GitlabAccount) -> Boolean): List<GitlabAccount> = mutableGitlabHostSettings.values.asSequence().flatMap { it.gitlabAccounts }.filter(filter).toList()

    fun getAllGitlabAccounts(): List<GitlabAccount> = mutableGitlabHostSettings.values.asSequence().flatMap { it.gitlabAccounts }.toList()

    fun deepCopy(): GitlabSettings {
        val newGitlabHostSettings = concurrentMapOf<String, GitlabHostSettings>()
        mutableGitlabHostSettings.forEach { newGitlabHostSettings[it.key] = it.value.deepCopy() }
        return GitlabSettings(newGitlabHostSettings)
    }
}
