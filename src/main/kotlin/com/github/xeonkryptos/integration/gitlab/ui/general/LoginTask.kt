package com.github.xeonkryptos.integration.gitlab.ui.general

import com.github.xeonkryptos.integration.gitlab.api.gitlab.GitlabUserApi
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings
import com.github.xeonkryptos.integration.gitlab.ui.clone.GitlabLoginData
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.function.Consumer

class LoginTask(project: Project, private val gitlabLoginData: GitlabLoginData, private val loginNotificationListener: Consumer<String?>? = null) : Task.Backgroundable(project,
                                                                                                                                                                        GitlabBundle.message("action.gitlab.accounts.user.information.download"),
                                                                                                                                                                        true,
                                                                                                                                                                        ALWAYS_BACKGROUND), Disposable {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val authenticationManager: AuthenticationManager = service()
    private val gitlabSettings: GitlabSettings = service<GitlabSettingsService>().getWorkableState()
    private val gitlabUserApi: GitlabUserApi = service()

    private val gitlabHostSettings: GitlabHostSettings =
            gitlabSettings.getOrCreateGitlabHostSettings(gitlabLoginData.gitlabHost).apply { disableSslVerification = gitlabLoginData.disableCertificateValidation }

    @Volatile
    var gitlabAccount: GitlabAccount? = null
        private set

    private var progressIndicator: ProgressIndicator? = null

    fun doLogin() {
        progressIndicator = EmptyProgressIndicator(ModalityState.NON_MODAL)
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(this, progressIndicator!!)
    }

    @RequiresBackgroundThread
    override fun run(indicator: ProgressIndicator) {
        try {
            indicator.checkCanceled()
            val gitlabUser: GitlabUser = gitlabUserApi.loadGitlabUser(gitlabHostSettings, gitlabLoginData.gitlabAccessToken)

            indicator.checkCanceled()
            val localGitlabAccount = gitlabHostSettings.createGitlabAccount(gitlabUser.userId, gitlabUser.username)
            gitlabAccount = localGitlabAccount

            authenticationManager.storeAuthentication(localGitlabAccount, gitlabLoginData.gitlabAccessToken)

            loginNotificationListener?.accept(null)

            indicator.checkCanceled()
        } catch (e: ProcessCanceledException) {
            onCancel()
        } catch (e: Exception) {
            LOG.warn("Log in with provided access token failed.", e)
            if (loginNotificationListener != null) {
                val errorMessage = GitlabBundle.message("credentials.incorrect", e.toString())
                loginNotificationListener.accept(errorMessage)
            }
            onCancel()
        }
    }

    @RequiresEdt
    override fun onCancel() {
        val localGitlabAccount: GitlabAccount? = gitlabAccount
        localGitlabAccount?.let {
            authenticationManager.deleteAuthenticationFor(it)
            gitlabHostSettings.removeGitlabAccount(it)
        }
    }

    override fun dispose() {
        progressIndicator?.cancel()
        onCancel()
    }
}