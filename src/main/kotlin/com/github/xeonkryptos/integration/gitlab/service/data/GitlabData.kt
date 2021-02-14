package com.github.xeonkryptos.integration.gitlab.service.data

import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.util.Observable
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.XCollection
import java.util.Objects

class GitlabData(activeGitlabAccount: GitlabAccount? = null) {

    val activeGitlabAccountObserver: Observable<GitlabAccount?> = Observable(activeGitlabAccount)
    var activeGitlabAccount by activeGitlabAccountObserver

    @XCollection(propertyElementName = "gitlabAccounts")
    private var mutableGitlabAccounts: MutableSet<GitlabAccount> = mutableSetOf()
    val gitlabAccounts: Set<GitlabAccount> = mutableGitlabAccounts

    init {
        activeGitlabAccountObserver.addObserver { _, newValue ->
            if (newValue != null) {
                mutableGitlabAccounts.add(newValue)
            }
        }
    }

    fun onLoadingFinished(project: Project) {
        activeGitlabAccount?.let {
            // Replace instance in set of the currently active account to use the same instance
            mutableGitlabAccounts.remove(it)
            mutableGitlabAccounts.add(it)
        }
        val authenticationManager = AuthenticationManager.getInstance(project)
        mutableGitlabAccounts.forEach { gitlabAccount ->
            gitlabAccount.signedInObservable.addObserver { _, newValue ->
                // The account currently set to be active is signed out now. Need to replace it with a still signed in account
                if (!newValue && activeGitlabAccount == gitlabAccount) {
                    activeGitlabAccount = mutableGitlabAccounts.firstOrNull { it.signedIn }
                }
            }
            if (!authenticationManager.hasAuthenticationTokenFor(gitlabAccount)) {
                gitlabAccount.signedIn = false
            }
        }
    }

    fun removeGitlabAccount(gitlabAccount: GitlabAccount) {
        signOut(gitlabAccount)
        mutableGitlabAccounts.remove(gitlabAccount)
        if (activeGitlabAccount == gitlabAccount) {
            activeGitlabAccount = mutableGitlabAccounts.firstOrNull { it.signedIn }
        }
    }

    fun signOut(gitlabAccount: GitlabAccount) {
        val localActiveGitlabAccount = activeGitlabAccount
        if (localActiveGitlabAccount == gitlabAccount) {
            localActiveGitlabAccount.signedIn = false
        } else {
            mutableGitlabAccounts.asSequence().filter { gitlabAccount == it }.firstOrNull()?.signedIn = false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GitlabData) return false
        return activeGitlabAccount == other.activeGitlabAccount && mutableGitlabAccounts == other.mutableGitlabAccounts
    }

    override fun hashCode() = Objects.hash(activeGitlabAccount, mutableGitlabAccounts)
}
