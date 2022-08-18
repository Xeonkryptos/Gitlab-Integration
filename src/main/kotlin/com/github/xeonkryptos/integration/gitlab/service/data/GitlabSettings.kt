package com.github.xeonkryptos.integration.gitlab.service.data

data class GitlabSettings(internal val settings: SerializableGitlabSettings) {

    val gitlabHostSettings: Map<String, GitlabHostSettings>
        get() = settings.mutableGitlabHostSettings.mapValues { it.value.gitlabHostSettings }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
    fun getOrCreateGitlabHostSettings(gitlabHost: String) =
            (settings.mutableGitlabHostSettings as java.util.Map<String, SerializableGitlabHostSettings>).computeIfAbsent(gitlabHost) { SerializableGitlabHostSettings(it) }.gitlabHostSettings

    fun updateWith(newSettings: GitlabSettings) {
        settings.mutableGitlabHostSettings.filterKeys { newSettings.containsGitlabHostSettings(it) }
            .forEach { it.value.gitlabHostSettings.updateWith(newSettings.settings.mutableGitlabHostSettings[it.key]!!.gitlabHostSettings) }
        newSettings.settings.mutableGitlabHostSettings.filterKeys { !settings.mutableGitlabHostSettings.containsKey(it) }.forEach { settings.mutableGitlabHostSettings[it.key] = it.value }
        gitlabHostSettings.keys.filter { !newSettings.containsGitlabHostSettings(it) }.forEach { settings.mutableGitlabHostSettings.remove(it) }
    }

    fun isModified(gitlabSettings: GitlabSettings) = settings.isModified(gitlabSettings.settings)

    fun containsGitlabHostSettings(gitlabHost: String): Boolean = settings.mutableGitlabHostSettings.containsKey(gitlabHost)

    fun removeGitlabHostSettings(gitlabHost: String) {
        settings.mutableGitlabHostSettings.remove(gitlabHost)
    }

    fun hasGitlabAccountBy(filter: (GitlabAccount) -> Boolean): Boolean = getFirstGitlabAccountBy(filter) != null

    fun getFirstGitlabAccountBy(filter: (GitlabAccount) -> Boolean): GitlabAccount? =
            settings.mutableGitlabHostSettings.values.asSequence().flatMap { it.mutableGitlabAccounts }.map { it.gitlabAccount }.firstOrNull(filter)

    fun getAllGitlabAccountsBy(filter: (GitlabAccount) -> Boolean): List<GitlabAccount> =
            settings.mutableGitlabHostSettings.values.asSequence().flatMap { it.mutableGitlabAccounts }.map { it.gitlabAccount }.filter(filter).toList()

    fun getAllGitlabAccounts(): List<GitlabAccount> = settings.mutableGitlabHostSettings.values.asSequence().flatMap { it.mutableGitlabAccounts }.map { it.gitlabAccount }.toList()

    fun deepCopy() = GitlabSettings(settings.deepCopy())
}
