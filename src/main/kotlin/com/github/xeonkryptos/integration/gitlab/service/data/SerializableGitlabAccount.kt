package com.github.xeonkryptos.integration.gitlab.service.data

import com.intellij.util.xmlb.annotations.Attribute

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
data class SerializableGitlabAccount(@Attribute var userId: Long = -1, @Attribute var assignedGitlabHost: String = "", @Attribute var username: String = "") {

    @Attribute
    var resolveOnlyOwnProjects: Boolean = false
    @Attribute
    var useSSH: Boolean = true

    val gitlabAccount by lazy { GitlabAccount(this) }

    fun isModified(gitlabAccount: SerializableGitlabAccount): Boolean = this != gitlabAccount || resolveOnlyOwnProjects != gitlabAccount.resolveOnlyOwnProjects || useSSH != gitlabAccount.useSSH

    fun deepCopy() = SerializableGitlabAccount(userId, assignedGitlabHost, username).apply {
        resolveOnlyOwnProjects = this@SerializableGitlabAccount.resolveOnlyOwnProjects
        useSSH = this@SerializableGitlabAccount.useSSH
    }
}
