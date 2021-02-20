package com.github.xeonkryptos.integration.gitlab.internal.messaging

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.util.messages.Topic

/**
 * @author Xeonkryptos
 * @since 19.02.2021
 */
interface GitlabLoginChangeNotifier {

    companion object {

        @JvmStatic
        val LOGIN_STATE_CHANGED_TOPIC: Topic<GitlabLoginChangeNotifier> = Topic.create("Changed login state", GitlabLoginChangeNotifier::class.java)
    }

    fun onSignIn(gitlabAccount: GitlabAccount) {}

    fun onSignOut(gitlabAccount: GitlabAccount) {}
}
