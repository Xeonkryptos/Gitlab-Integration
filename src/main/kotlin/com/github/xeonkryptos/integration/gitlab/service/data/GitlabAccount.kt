package com.github.xeonkryptos.integration.gitlab.service.data

import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
data class GitlabAccount(@Volatile var userId: Long? = null, @Volatile var username: String = "", @Volatile var useSSH: Boolean = false) {

    @Volatile
    @Transient
    private var gitlabHostSettingsOwner: GitlabHostSettings? = null

    @Volatile
    @OptionTag
    var resolveOnlyOwnProjects: Boolean = false

    constructor(gitlabHostSettings: GitlabHostSettings, username: String) : this(null, username) {
        this.gitlabHostSettingsOwner = gitlabHostSettings
    }

    fun updateWith(gitlabAccount: GitlabAccount) {
        resolveOnlyOwnProjects = gitlabAccount.resolveOnlyOwnProjects
    }

    fun delete() {
        gitlabHostSettingsOwner?.removeGitlabAccount(this)
    }

    fun isModified(gitlabAccount: GitlabAccount): Boolean =
        this != gitlabAccount || resolveOnlyOwnProjects != gitlabAccount.resolveOnlyOwnProjects || gitlabHostSettingsOwner?.gitlabHost != gitlabAccount.gitlabHostSettingsOwner?.gitlabHost

    fun getGitlabHost(): String = gitlabHostSettingsOwner!!.gitlabHost

    fun getTargetGitlabHost(): String {
        val gitlabHost = getGitlabHost()
        return if (gitlabHost.endsWith("/")) gitlabHost.substring(0, gitlabHost.length - 1) else gitlabHost
    }

    @Transient
    internal fun getGitlabHostSettingsOwner() = gitlabHostSettingsOwner

    @Transient
    internal fun setGitlabHostSettingsOwner(gitlabHostSettingsOwner: GitlabHostSettings) {
        this.gitlabHostSettingsOwner = gitlabHostSettingsOwner
    }

    fun deepCopy(): GitlabAccount {
        val newGitlabAccount = GitlabAccount(userId, username, useSSH)
        newGitlabAccount.resolveOnlyOwnProjects = resolveOnlyOwnProjects
        return newGitlabAccount
    }
}
