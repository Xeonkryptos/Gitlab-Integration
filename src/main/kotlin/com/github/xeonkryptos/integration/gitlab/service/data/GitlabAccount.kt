package com.github.xeonkryptos.integration.gitlab.service.data

import com.github.xeonkryptos.integration.gitlab.util.Observable

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
data class GitlabAccount(var username: String) {

    internal var gitlabHostSettingsOwner: GitlabHostSettings? = null

    val signedInObservable: Observable<Boolean> = Observable(false)
    var signedIn by signedInObservable

    var resolveOnlyOwnProjects: Boolean = false

    internal constructor(gitlabHostSettings: GitlabHostSettings, username: String) : this(username) {
        this.gitlabHostSettingsOwner = gitlabHostSettings
    }

    fun delete() {
        gitlabHostSettingsOwner?.removeGitlabAccount(this)
    }

    fun updateWith(gitlabAccount: GitlabAccount) {
        signedIn = gitlabAccount.signedIn
        resolveOnlyOwnProjects = gitlabAccount.resolveOnlyOwnProjects
    }

    fun getGitlabHost(): String = gitlabHostSettingsOwner!!.gitlabHost

    fun getNormalizeGitlabHost(): String = gitlabHostSettingsOwner!!.getNormalizeGitlabHost()
}
