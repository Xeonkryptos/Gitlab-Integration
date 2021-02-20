package com.github.xeonkryptos.integration.gitlab.service.data

import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.util.invokeOnDispatchThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.MessageBus
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
data class GitlabAccount(@Volatile var username: String = "") {

    private val messageBus: MessageBus = ApplicationManager.getApplication().messageBus

    @Volatile
    @Transient
    private var gitlabHostSettingsOwner: GitlabHostSettings? = null

    @Volatile
    @OptionTag
    var signedIn: Boolean = false
        set(value) {
            field = value

            val publisher = messageBus.syncPublisher(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC)
            if (field) publisher.onSignIn(this)
            else publisher.onSignOut(this)
        }

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
        signedIn = gitlabAccount.signedIn
        resolveOnlyOwnProjects = gitlabAccount.resolveOnlyOwnProjects
    }

    fun isModified(gitlabAccount: GitlabAccount): Boolean =
        this != gitlabAccount || signedIn != gitlabAccount.signedIn || resolveOnlyOwnProjects != gitlabAccount.resolveOnlyOwnProjects || gitlabHostSettingsOwner?.gitlabHost != gitlabAccount.gitlabHostSettingsOwner?.gitlabHost

    fun getGitlabHost(): String = gitlabHostSettingsOwner!!.gitlabHost

    fun getNormalizeGitlabHost(): String = gitlabHostSettingsOwner!!.getNormalizeGitlabHost()

    fun setGitlabHostSettingsOwner(gitlabHostSettingsOwner: GitlabHostSettings) {
        this.gitlabHostSettingsOwner = gitlabHostSettingsOwner
    }

    fun deepCopy(): GitlabAccount {
        val newGitlabAccount = GitlabAccount(username)
        newGitlabAccount.signedIn = signedIn
        newGitlabAccount.resolveOnlyOwnProjects = resolveOnlyOwnProjects
        return newGitlabAccount
    }
}
