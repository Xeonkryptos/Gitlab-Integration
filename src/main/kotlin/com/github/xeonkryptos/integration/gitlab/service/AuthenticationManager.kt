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

    fun storeAuthenticationPassword(gitlabAccount: GitlabAccount, password: CharArray) {
        storePasswordFor(gitlabAccount, password)
    }

    fun deleteAuthenticationFor(gitlabAccount: GitlabAccount) {
        deleteTokenFor(gitlabAccount)
    }

    fun hasAuthenticationTokenFor(gitlabAccount: GitlabAccount) = getAuthenticationTokenFor(gitlabAccount) != null || getAuthenticationPasswordFor(gitlabAccount) != null

    fun getAuthenticationTokenFor(gitlabAccount: GitlabAccount): String? {
        val credentialAttributes: CredentialAttributes = createTokenCredentialAttributes(gitlabAccount)
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    fun getAuthenticationPasswordFor(gitlabAccount: GitlabAccount): String? {
        val credentialAttributes = createPasswordCredentialAttributes(gitlabAccount)
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    private fun storeTokenFor(gitlabAccount: GitlabAccount, gitlabAccessToken: String) {
        val credentialAttributes = createTokenCredentialAttributes(gitlabAccount)
        PasswordSafe.instance.setPassword(credentialAttributes, gitlabAccessToken)
    }

    private fun storePasswordFor(gitlabAccount: GitlabAccount, password: CharArray) {
        val credentialAttributes = createPasswordCredentialAttributes(gitlabAccount)
        PasswordSafe.instance.set(credentialAttributes, Credentials(gitlabAccount.username, password))
    }

    private fun deleteTokenFor(gitlabAccount: GitlabAccount) {
        val credentialAttributes = createTokenCredentialAttributes(gitlabAccount)
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    private fun createTokenCredentialAttributes(gitlabAccount: GitlabAccount): CredentialAttributes {
        val gitlabTokenServiceName = generateServiceName("Gitlab Token", "${gitlabAccount.getGitlabHost()}->${gitlabAccount.username}")
        return CredentialAttributes(gitlabTokenServiceName)
    }

    private fun createPasswordCredentialAttributes(gitlabAccount: GitlabAccount): CredentialAttributes {
        val gitlabTokenServiceName = generateServiceName("Gitlab Password", "${gitlabAccount.getGitlabHost()}->${gitlabAccount.username}")
        return CredentialAttributes(gitlabTokenServiceName)
    }
}
