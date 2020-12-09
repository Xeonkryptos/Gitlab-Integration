package com.github.xeonkryptos.integration.gitlab.service

class GitlabData(activeGitlabHost: String? = null, private val mutableGitlabHosts: MutableSet<String> = HashSet()) {

    var activeGitlabHost: String? = activeGitlabHost
        set(gitlabHost) {
            field = gitlabHost
            field?.let { mutableGitlabHosts.add(it) }
        }

    val gitlabHosts: Set<String> = mutableGitlabHosts
}
