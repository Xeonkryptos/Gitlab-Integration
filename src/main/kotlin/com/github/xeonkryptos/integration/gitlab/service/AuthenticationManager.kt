package com.github.xeonkryptos.integration.gitlab.service

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project

/**
 * @author Xeonkryptos
 * @since 01.02.2021
 */
class AuthenticationManager private constructor(@Suppress("UNUSED_PARAMETER") project: Project) {

    companion object {

        @JvmStatic
        private var instance: AuthenticationManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(project: Project): AuthenticationManager {
            if (instance == null) {
                instance = AuthenticationManager(project)
            }
            return instance!!
        }
    }

    fun storeAuthentication(gitlabAccount: GitlabAccount, gitlabAccessToken: String) {
        storeTokenFor(gitlabAccount, gitlabAccessToken)
    }

    fun deleteAuthenticationFor(gitlabAccount: GitlabAccount) {
        deleteTokenFor(gitlabAccount)
    }

    fun hasAuthenticationTokenFor(gitlabAccount: GitlabAccount) = getAuthenticationTokenFor(gitlabAccount) != null

    fun getAuthenticationTokenFor(gitlabAccount: GitlabAccount): String? {
        val credentialAttributes: CredentialAttributes = createCredentialAttributes(gitlabAccount)
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    private fun storeTokenFor(gitlabAccount: GitlabAccount, gitlabAccessToken: String) {
        val credentialAttributes = createCredentialAttributes(gitlabAccount)
        PasswordSafe.instance.set(credentialAttributes, Credentials(gitlabAccount.gitlabHost, gitlabAccessToken))
    }

    private fun deleteTokenFor(gitlabAccount: GitlabAccount) {
        val credentialAttributes = createCredentialAttributes(gitlabAccount)
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    private fun createCredentialAttributes(gitlabAccount: GitlabAccount): CredentialAttributes {
        val gitlabTokenServiceName = generateServiceName("Gitlab Token", "${gitlabAccount.gitlabHost}->${gitlabAccount.username}")
        return CredentialAttributes(gitlabTokenServiceName)
    }
}
