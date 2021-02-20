package com.github.xeonkryptos.integration.gitlab.service.data

import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
data class GitlabAccount(@Volatile var username: String = "") {

    @Volatile
    @Transient
    private var gitlabHostSettingsOwner: GitlabHostSettings? = null

    @Volatile
    @OptionTag
    var resolveOnlyOwnProjects: Boolean = false

    constructor(gitlabHostSettings: GitlabHostSettings, username: String) : this(username) {
        this.gitlabHostSettingsOwner = gitlabHostSettings
    }

    fun delete() {
        gitlabHostSettingsOwner?.removeGitlabAccount(this)
    }

    fun updateWith(gitlabAccount: GitlabAccount) {
        resolveOnlyOwnProjects = gitlabAccount.resolveOnlyOwnProjects
    }

    fun isModified(gitlabAccount: GitlabAccount): Boolean =
        this != gitlabAccount || resolveOnlyOwnProjects != gitlabAccount.resolveOnlyOwnProjects || gitlabHostSettingsOwner?.gitlabHost != gitlabAccount.gitlabHostSettingsOwner?.gitlabHost

    fun getGitlabHost(): String = gitlabHostSettingsOwner!!.gitlabHost

    fun getNormalizeGitlabHost(): String = gitlabHostSettingsOwner!!.getNormalizeGitlabHost()

    fun setGitlabHostSettingsOwner(gitlabHostSettingsOwner: GitlabHostSettings) {
        this.gitlabHostSettingsOwner = gitlabHostSettingsOwner
    }

    fun deepCopy(): GitlabAccount {
        val newGitlabAccount = GitlabAccount(username)
        newGitlabAccount.resolveOnlyOwnProjects = resolveOnlyOwnProjects
        return newGitlabAccount
    }
}
