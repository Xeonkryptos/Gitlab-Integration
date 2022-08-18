package com.github.xeonkryptos.integration.gitlab.service.data

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection

/**
 * @author Xeonkryptos
 * @since 16.02.2021
 */
data class SerializableGitlabHostSettings(@Attribute var gitlabHost: String = "") {

    @XCollection(propertyElementName = "gitlabAccounts")
    var mutableGitlabAccounts: MutableSet<SerializableGitlabAccount> = mutableSetOf()
    @Attribute
    var disableSslVerification: Boolean = false

    val gitlabHostSettings by lazy { GitlabHostSettings(this) }

    fun isModified(gitlabHostSetting: SerializableGitlabHostSettings): Boolean {
        if (this != gitlabHostSetting || disableSslVerification != gitlabHostSetting.disableSslVerification) return true
        val accounts: List<SerializableGitlabAccount> = ArrayList(mutableGitlabAccounts)
        val otherAccounts: List<SerializableGitlabAccount> = ArrayList(gitlabHostSetting.mutableGitlabAccounts)
        if (accounts.size != otherAccounts.size) {
            return true
        }
        return accounts.withIndex().any {
            val otherAccount = otherAccounts[it.index]
            return@any it.value.isModified(otherAccount)
        }
    }

    fun deepCopy() = SerializableGitlabHostSettings(gitlabHost).apply {
        mutableGitlabAccounts = this@SerializableGitlabHostSettings.mutableGitlabAccounts.map { it.deepCopy() }.toMutableSet()
        disableSslVerification = this@SerializableGitlabHostSettings.disableSslVerification
    }
}
