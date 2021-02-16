package com.github.xeonkryptos.integration.gitlab.service.data

import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.XCollection

/**
 * @author Xeonkryptos
 * @since 16.02.2021
 */
data class GitlabHostSettings(var gitlabHost: String) {

    private val gitlabAccountStateListeners: MutableSet<() -> Unit> = mutableSetOf()

    @XCollection(propertyElementName = "gitlabHostSettings")
    private var mutableGitlabAccounts: MutableSet<GitlabAccount> = mutableSetOf()
    val gitlabAccounts: Set<GitlabAccount>
        get() = mutableGitlabAccounts

    var disableSslVerification: Boolean = false

    fun createGitlabAccount(username: String): GitlabAccount {
        val gitlabAccount = GitlabAccount(this, username)
        mutableGitlabAccounts.add(gitlabAccount)
        return gitlabAccount
    }

    fun removeGitlabAccount(gitlabAccount: GitlabAccount) {
        mutableGitlabAccounts.remove(gitlabAccount)
        gitlabAccountStateListeners.forEach { it.invoke() }
    }

    fun updateWith(gitlabHostSetting: GitlabHostSettings) {
        disableSslVerification = gitlabHostSetting.disableSslVerification
        mutableGitlabAccounts.retainAll(gitlabHostSetting.mutableGitlabAccounts)

        val gitlabAccountsMap = mutableGitlabAccounts.associateBy { it }
        gitlabHostSetting.mutableGitlabAccounts.forEach {
            if (!gitlabAccountsMap.containsKey(it)) {
                mutableGitlabAccounts.add(it)
            } else {
                gitlabAccountsMap[it]!!.updateWith(it)
            }
        }
    }

    internal fun onLoadingFinished(project: Project) {
        val authenticationManager = AuthenticationManager.getInstance(project)
        gitlabAccounts.forEach { gitlabAccount ->
            gitlabAccount.gitlabHostSettingsOwner = this
            if (!authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
                gitlabAccount.signedIn = false
            }
            gitlabAccount.signedInObservable.addObserver { _, _ -> gitlabAccountStateListeners.forEach { it.invoke() } }
        }
    }

    fun addGitlabAccountStateListener(listener: () -> Unit) {
        gitlabAccountStateListeners.add(listener)
    }

    fun getNormalizeGitlabHost(): String {
        var normalizedGitlabHost = gitlabHost.replace("https://", "").replace("http://", "")
        if (normalizedGitlabHost.endsWith('/')) {
            normalizedGitlabHost = normalizedGitlabHost.substring(0, normalizedGitlabHost.length - 1)
        }
        return normalizedGitlabHost
    }
}
