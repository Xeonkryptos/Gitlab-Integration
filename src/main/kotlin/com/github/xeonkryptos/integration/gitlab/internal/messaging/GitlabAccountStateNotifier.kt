package com.github.xeonkryptos.integration.gitlab.internal.messaging

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.util.messages.Topic

/**
 * @author Xeonkryptos
 * @since 19.02.2021
 */
interface GitlabAccountStateNotifier {

    companion object {

        @JvmStatic
        val ACCOUNT_STATE_TOPIC: Topic<GitlabAccountStateNotifier> = Topic.create("Gitlab account state change", GitlabAccountStateNotifier::class.java)
    }

    fun onGitlabAccountCreated(gitlabAccount: GitlabAccount) {}

    fun onGitlabAccountDeleted(gitlabAccount: GitlabAccount) {}
}
