package com.github.xeonkryptos.integration.gitlab.service

import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.MessageBus

/**
 * @author Xeonkryptos
 * @since 01.02.2021
 */
class AuthenticationManager {

    private val messageBus: MessageBus = ApplicationManager.getApplication().messageBus

    fun storeAuthentication(gitlabAccount: GitlabAccount, gitlabAccessToken: String) {
        storeTokenFor(gitlabAccount, gitlabAccessToken)

        messageBus.syncPublisher(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC).onSignIn(gitlabAccount)
    }

    fun deleteAuthenticationFor(gitlabAccount: GitlabAccount) {
        deleteTokenFor(gitlabAccount)

        messageBus.syncPublisher(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC).onSignOut(gitlabAccount)
    }

    fun hasAuthenticationTokenFor(gitlabAccount: GitlabAccount) = getAuthenticationTokenFor(gitlabAccount) != null

    fun getAuthenticationTokenFor(gitlabAccount: GitlabAccount): String? {
        val credentialAttributes: CredentialAttributes = createTokenCredentialAttributes(gitlabAccount)
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    private fun storeTokenFor(gitlabAccount: GitlabAccount, gitlabAccessToken: String) {
        val credentialAttributes = createTokenCredentialAttributes(gitlabAccount)
        PasswordSafe.instance.setPassword(credentialAttributes, gitlabAccessToken)
    }

    private fun deleteTokenFor(gitlabAccount: GitlabAccount) {
        val credentialAttributes = createTokenCredentialAttributes(gitlabAccount)
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    private fun createTokenCredentialAttributes(gitlabAccount: GitlabAccount): CredentialAttributes {
        val gitlabTokenServiceName = generateServiceName("Gitlab Token", "${gitlabAccount.getGitlabHost()}->${gitlabAccount.username}")
        return CredentialAttributes(gitlabTokenServiceName)
    }
}
