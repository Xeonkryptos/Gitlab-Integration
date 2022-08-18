package com.github.xeonkryptos.integration.gitlab.service.data

import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabAccountStateNotifier
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabHostStateNotifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.MessageBus

/**
 * @author Xeonkryptos
 * @since 16.02.2021
 */
data class GitlabHostSettings(internal val hostSettings: SerializableGitlabHostSettings) {

    private val messageBus: MessageBus = ApplicationManager.getApplication().messageBus

    val gitlabHost: String by hostSettings::gitlabHost
    val gitlabAccounts: Set<GitlabAccount>
        get() = hostSettings.mutableGitlabAccounts.map { it.gitlabAccount }.toSet()

    var disableSslVerification: Boolean by hostSettings::disableSslVerification

    fun createGitlabAccount(userId: Long, username: String, silent: Boolean = false): GitlabAccount {
        val serializableGitlabAccount = SerializableGitlabAccount(userId, hostSettings.gitlabHost, username)
        return if (hostSettings.mutableGitlabAccounts.add(serializableGitlabAccount)) {
            if (!silent) {
                val publisher = messageBus.syncPublisher(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC)
                publisher.onGitlabAccountCreated(serializableGitlabAccount.gitlabAccount)
            }

            serializableGitlabAccount.gitlabAccount
        } else {
            hostSettings.mutableGitlabAccounts.find { it.username == username }!!.gitlabAccount
        }
    }

    fun removeGitlabAccount(gitlabAccount: GitlabAccount, silent: Boolean = false) {
        hostSettings.mutableGitlabAccounts.remove(gitlabAccount.account)

        if (!silent) {
            val publisher = messageBus.syncPublisher(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC)
            publisher.onGitlabAccountDeleted(gitlabAccount)
        }

        if (hostSettings.mutableGitlabAccounts.isEmpty()) {
            val publisher = messageBus.syncPublisher(GitlabHostStateNotifier.HOST_STATE_TOPIC)
            publisher.onGitlabHostsWithoutAnyAccounts(this)
        }
    }

    fun updateWith(gitlabHostSetting: GitlabHostSettings) {
        disableSslVerification = gitlabHostSetting.disableSslVerification
        hostSettings.mutableGitlabAccounts.retainAll(gitlabHostSetting.hostSettings.mutableGitlabAccounts)

        val gitlabAccountsMap = hostSettings.mutableGitlabAccounts.associateBy { it }
        gitlabHostSetting.hostSettings.mutableGitlabAccounts.forEach {
            if (!gitlabAccountsMap.containsKey(it)) {
                hostSettings.mutableGitlabAccounts.add(it)
            } else {
                gitlabAccountsMap[it]!!.gitlabAccount.updateWith(it.gitlabAccount)
            }
        }
    }

    fun isModified(gitlabHostSetting: GitlabHostSettings) = hostSettings.isModified(gitlabHostSetting.hostSettings)

    fun deepCopy() = GitlabHostSettings(hostSettings.deepCopy())
}
