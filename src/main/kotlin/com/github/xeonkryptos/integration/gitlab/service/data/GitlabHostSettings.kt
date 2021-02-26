package com.github.xeonkryptos.integration.gitlab.service.data

import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabAccountStateNotifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.MessageBus
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.XCollection
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Xeonkryptos
 * @since 16.02.2021
 */
data class GitlabHostSettings(@Volatile var gitlabHost: String = "") {

    private val messageBus: MessageBus = ApplicationManager.getApplication().messageBus

    @Volatile
    @XCollection(propertyElementName = "gitlabAccounts")
    private var mutableGitlabAccounts: MutableSet<GitlabAccount> = ConcurrentHashMap.newKeySet()
    val gitlabAccounts: Set<GitlabAccount> = mutableGitlabAccounts

    @Volatile
    @OptionTag
    var disableSslVerification: Boolean = false

    fun createGitlabAccount(username: String): GitlabAccount {
        val gitlabAccount = GitlabAccount(this, username)
        return if (mutableGitlabAccounts.add(gitlabAccount)) {
            val publisher = messageBus.syncPublisher(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC)
            publisher.onGitlabAccountCreated(gitlabAccount)

            gitlabAccount
        } else {
            mutableGitlabAccounts.find { it.username == username }!!
        }
    }

    fun removeGitlabAccount(gitlabAccount: GitlabAccount) {
        mutableGitlabAccounts.remove(gitlabAccount)

        val publisher = messageBus.syncPublisher(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC)
        publisher.onGitlabAccountDeleted(gitlabAccount)
    }

    fun updateWith(gitlabHostSetting: GitlabHostSettings) {
        disableSslVerification = gitlabHostSetting.disableSslVerification
        mutableGitlabAccounts.retainAll(gitlabHostSetting.mutableGitlabAccounts)

        val gitlabAccountsMap = mutableGitlabAccounts.associateBy { it }
        gitlabHostSetting.mutableGitlabAccounts.forEach { gitlabAccount ->
            if (!gitlabAccountsMap.containsKey(gitlabAccount)) {
                mutableGitlabAccounts.add(gitlabAccount)
            } else {
                gitlabAccountsMap[gitlabAccount]!!.updateWith(gitlabAccount)
            }
        }
    }

    internal fun onLoadingFinished() {
        gitlabAccounts.forEach { it.setGitlabHostSettingsOwner(this) }
    }

    fun isModified(gitlabHostSetting: GitlabHostSettings): Boolean {
        if (this != gitlabHostSetting || disableSslVerification != gitlabHostSetting.disableSslVerification) return true
        val accounts: List<GitlabAccount> = ArrayList(mutableGitlabAccounts)
        val otherAccounts: List<GitlabAccount> = ArrayList(gitlabHostSetting.mutableGitlabAccounts)
        return accounts.withIndex().any {
            val otherAccount = otherAccounts[it.index]
            return@any it.value.isModified(otherAccount)
        }
    }

    fun getNormalizeGitlabHost(): String {
        var normalizedGitlabHost = gitlabHost.replace("https://", "").replace("http://", "")
        if (normalizedGitlabHost.endsWith('/')) {
            normalizedGitlabHost = normalizedGitlabHost.substring(0, normalizedGitlabHost.length - 1)
        }
        return normalizedGitlabHost
    }

    fun deepCopy(): GitlabHostSettings {
        val newGitlabAccounts: MutableSet<GitlabAccount> = ConcurrentHashMap.newKeySet()
        val newGitlabHostSettings = GitlabHostSettings(gitlabHost)
        mutableGitlabAccounts.forEach { currentGitlabAccount ->
            val newGitlabAccount = currentGitlabAccount.deepCopy()
            newGitlabAccount.setGitlabHostSettingsOwner(newGitlabHostSettings)
            newGitlabAccounts.add(newGitlabAccount)
        }
        newGitlabHostSettings.mutableGitlabAccounts.addAll(newGitlabAccounts)
        return newGitlabHostSettings
    }
}