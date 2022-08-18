package com.github.xeonkryptos.integration.gitlab.service.data

import com.intellij.util.xmlb.annotations.XCollection

data class SerializableGitlabSettings(@XCollection(propertyElementName = "gitlabHosts") var mutableGitlabHostSettings: MutableMap<String, SerializableGitlabHostSettings> = mutableMapOf()) {

    val gitlabSettings by lazy { GitlabSettings(this) }

    fun isModified(gitlabSettings: SerializableGitlabSettings): Boolean {
        if (this != gitlabSettings) return true
        return mutableGitlabHostSettings.any { entry -> entry.value.isModified(gitlabSettings.mutableGitlabHostSettings[entry.key]!!) }
    }

    fun deepCopy() = SerializableGitlabSettings(mutableGitlabHostSettings.mapValues { it.value.deepCopy() }.toMutableMap())
}
