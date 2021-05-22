package com.github.xeonkryptos.integration.gitlab.service.data

import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.rd.util.concurrentMapOf

data class GitlabSettings(@Volatile @XCollection(propertyElementName = "gitlabHosts") private var mutableGitlabHostSettings: MutableMap<String, GitlabHostSettings> = concurrentMapOf()) {

    val gitlabHostSettings: Map<String, GitlabHostSettings> = mutableGitlabHostSettings

    fun onLoadingFinished() {
        mutableGitlabHostSettings.values.forEach { it.onLoadingFinished() }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
    fun getOrCreateGitlabHostSettings(gitlabHost: String): GitlabHostSettings {
        return (mutableGitlabHostSettings as java.util.Map<String, GitlabHostSettings>).computeIfAbsent(gitlabHost) { GitlabHostSettings(it) }
    }

    fun registerGitlabAccount(gitlabAccount: GitlabAccount) {
        val gitlabHost = gitlabAccount.getGitlabHost()

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
        val gitlabHostSettings = (mutableGitlabHostSettings as java.util.Map<String, GitlabHostSettings>).computeIfAbsent(gitlabHost) { gitlabAccount.getGitlabHostSettingsOwner()!! }
        if (gitlabHostSettings != gitlabAccount.getGitlabHostSettingsOwner()) {
            gitlabHostSettings.addGitlabAccount(gitlabAccount)
        }
    }

    fun updateWith(newSettings: GitlabSettings) {
        gitlabHostSettings.filterKeys { newSettings.containsGitlabHostSettings(it) }.forEach { it.value.updateWith(newSettings.gitlabHostSettings[it.key]!!) }
        newSettings.gitlabHostSettings.filterKeys { !gitlabHostSettings.containsKey(it) }.forEach { mutableGitlabHostSettings[it.key] = it.value }
        gitlabHostSettings.keys.filter { !newSettings.containsGitlabHostSettings(it) }.forEach { mutableGitlabHostSettings.remove(it) }
    }

    fun isModified(gitlabSettings: GitlabSettings): Boolean {
        if (this != gitlabSettings) return true
        return mutableGitlabHostSettings.any { entry -> entry.value.isModified(gitlabSettings.mutableGitlabHostSettings[entry.key]!!) }
    }

    fun containsGitlabHostSettings(gitlabHost: String): Boolean = mutableGitlabHostSettings.containsKey(gitlabHost)

    fun removeGitlabHostSettings(gitlabHost: String) {
        mutableGitlabHostSettings.remove(gitlabHost)?.gitlabAccounts?.forEach { it.delete() }
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
