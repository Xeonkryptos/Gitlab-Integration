package com.github.xeonkryptos.integration.gitlab.service.data

import com.github.xeonkryptos.integration.gitlab.util.Observable

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
data class GitlabAccount(var gitlabHost: String? = null, var username: String? = null) {

    val signedInObservable: Observable<Boolean> = Observable(false)
    var signedIn by signedInObservable

    fun getNormalizeGitlabHost(): String? {
        var normalizedGitlabHost = gitlabHost?.replace("https://", "")?.replace("http://", "")
        if (normalizedGitlabHost?.endsWith('/') == true) {
            normalizedGitlabHost = normalizedGitlabHost.substring(0, normalizedGitlabHost.length - 1)
        }
        return normalizedGitlabHost
    }
}
