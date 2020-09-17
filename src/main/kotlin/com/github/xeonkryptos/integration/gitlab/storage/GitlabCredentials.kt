package com.github.xeonkryptos.integration.gitlab.storage

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class GitlabCredentials private constructor() {

    companion object {

        @JvmStatic
        fun getTokenFor(gitlabHost: String): String? {
            val credentialAttributes: CredentialAttributes = createCredentialAttributes(gitlabHost)
            return PasswordSafe.instance.getPassword(credentialAttributes)
        }

        @JvmStatic
        fun storeTokenFor(gitlabHost: String, gitlabAccessToken: String) {
            val credentialAttributes = createCredentialAttributes(gitlabHost)
            PasswordSafe.instance.set(credentialAttributes, Credentials(gitlabHost, gitlabAccessToken))
        }

        @JvmStatic
        private fun createCredentialAttributes(key: String): CredentialAttributes {
            val gitlabTokenServiceName = generateServiceName("Gitlab Token", key)
            return CredentialAttributes(gitlabTokenServiceName)
        }
    }
}
