package com.github.xeonkryptos.integration.gitlab.internal.messaging

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.intellij.util.messages.Topic

interface GitlabHostStateNotifier {

    companion object {

        @JvmField
        val HOST_STATE_TOPIC: Topic<GitlabHostStateNotifier> = Topic.create("Gitlab host state change", GitlabHostStateNotifier::class.java)
    }

    fun onGitlabHostsWithoutAnyAccounts(gitlabHostSettings: GitlabHostSettings)
}