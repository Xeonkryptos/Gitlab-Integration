package com.github.xeonkryptos.integration.gitlab.service.data

import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
data class GitlabAccount(val account: SerializableGitlabAccount) {

    val userId: Long by account::userId
    val assignedGitlabHost: String by account::assignedGitlabHost
    val username: String by account::username

    var resolveOnlyOwnProjects: Boolean by account::resolveOnlyOwnProjects
    var useSSH: Boolean by account::useSSH

    fun updateWith(gitlabAccount: GitlabAccount) {
        resolveOnlyOwnProjects = gitlabAccount.resolveOnlyOwnProjects
        useSSH = gitlabAccount.useSSH
    }

    fun getGitlabHost(): String = account.assignedGitlabHost

    fun getGitlabDomain(): String = GitlabUtil.getGitlabDomain(account.assignedGitlabHost)

    fun getGitlabDomainWithoutPort(): String = GitlabUtil.convertToRepoUri(account.assignedGitlabHost).host

    fun getTargetGitlabHost(): String {
        val gitlabHost = getGitlabHost()
        return if (gitlabHost.endsWith("/")) gitlabHost.substring(0, gitlabHost.length - 1) else gitlabHost
    }

    fun deepCopy(): GitlabAccount = GitlabAccount(account.deepCopy())
}
