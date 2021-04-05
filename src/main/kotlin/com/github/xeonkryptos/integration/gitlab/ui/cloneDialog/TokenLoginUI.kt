package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

import com.github.xeonkryptos.integration.gitlab.api.GitlabApiManager
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.awt.Color
import javax.swing.JButton
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class TokenLoginUI @JvmOverloads constructor(project: Project, private val gitlabApiManager: GitlabApiManager, private val errorNotificationListener: Runnable? = null) {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val authenticationManager = AuthenticationManager.getInstance(project)

    private val gitlabSettings = GitlabSettingsService.getInstance(project).state

    private val gitlabHostTxtField: JBTextField = JBTextField().apply {
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (!disableCertificateValidationManuallySet) {
                    val skipCharacters = if (text.startsWith("http://")) "http://".length else if (text.startsWith("https://")) "https://".length else 0
                    val endOfHostname = text.indexOf('/', skipCharacters)
                    val domainWithProtocol = if (endOfHostname > -1) text.substring(0 until endOfHostname) else text

                    disableCertificateValidation = gitlabSettings.gitlabHostSettings[domainWithProtocol]?.disableSslVerification ?: false
                    if (checkBoxBuilder?.component?.isSelected != disableCertificateValidation) {
                        checkBoxBuilder?.component?.isSelected = disableCertificateValidation
                    }
                }
            }
        })
    }
    private val gitlabAccessTokenTxtField: JBTextField = JBTextField()

    private var checkBoxBuilder: CellBuilder<JBCheckBox>? = null
    private var disableCertificateValidationManuallySet: Boolean = false
    var disableCertificateValidation: Boolean = false

    private val errorLabel = JBLabel().apply {
        setAllowAutoWrapping(true)
        setCopyable(true)

        foreground = Color.RED
    }

    val tokenLoginPanel: DialogPanel

    private var progressIndicator: ProgressIndicator? = null
    private var backgroundTask: Task.Backgroundable? = null

    private var loginSucceeded: Boolean = false
    private lateinit var loginButtonCellBuilder: CellBuilder<JButton>

    init {
        tokenLoginPanel = panel(title = GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.text")) {
            row(GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.host")) { gitlabHostTxtField().applyIfEnabled().focused() }
            row(GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.token")) { gitlabAccessTokenTxtField() }
            row {
                checkBoxBuilder = checkBox(GitlabBundle.message("settings.general.table.column.certificates"), this@TokenLoginUI::disableCertificateValidation).applyToComponent {
                    addActionListener { disableCertificateValidationManuallySet = true }
                }
            }
            row {
                loginButtonCellBuilder = button(GitlabBundle.message("accounts.log.in")) {
                    errorLabel.isVisible = false

                    backgroundTask = LoginTask(project)
                    progressIndicator = EmptyProgressIndicator(ModalityState.NON_MODAL)
                    ProgressManager.getInstance().runProcessWithProgressAsynchronously(backgroundTask!!, progressIndicator!!)
                }.enableIf(AccessTokenLoginPredicate())
            }
            row { errorLabel(growPolicy = GrowPolicy.SHORT_TEXT, constraints = arrayOf(growY, pushY)) }
        }
    }

    fun cancel() {
        progressIndicator?.cancel()
        backgroundTask?.onCancel()
    }

    private inner class LoginTask(project: Project) : Task.Backgroundable(project, GitlabBundle.message("action.gitlab.accounts.user.information.download"), true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {

        @Volatile
        private var gitlabAccount: GitlabAccount? = null

        private val gitlabHost = gitlabHostTxtField.text
        private val gitlabAccessToken = gitlabAccessTokenTxtField.text
        private val gitlabHostSettings: GitlabHostSettings = gitlabSettings.getOrCreateGitlabHostSettings(gitlabHost).apply { disableSslVerification = this@TokenLoginUI.disableCertificateValidation }

        @RequiresBackgroundThread
        override fun run(indicator: ProgressIndicator) {
            try {
                indicator.checkCanceled()
                val gitlabUser: GitlabUser = gitlabApiManager.loadGitlabUser(gitlabHostSettings, gitlabAccessToken)

                indicator.checkCanceled()
                val localGitlabAccount = gitlabHostSettings.createGitlabAccount(gitlabUser.username)
                gitlabAccount = localGitlabAccount
                authenticationManager.storeAuthentication(localGitlabAccount, gitlabAccessToken)

                loginButtonCellBuilder.component.isEnabled = false

                indicator.checkCanceled()
            } catch (e: ProcessCanceledException) {
                onCancel()
            } catch (e: Exception) {
                LOG.error("Log in with provided access token failed.", e, "Host: $gitlabHost")
                errorLabel.text = GitlabBundle.message("action.gitlab.accounts.addGitlabAccountWithToken.failure", e.toString())
                errorLabel.isVisible = true
                errorNotificationListener?.run()
            }
        }

        @RequiresEdt
        override fun onCancel() {
            loginButtonCellBuilder.enabled(true)
            val localGitlabAccount: GitlabAccount? = gitlabAccount
            localGitlabAccount?.let {
                authenticationManager.deleteAuthenticationFor(it)
                it.delete()
            }
            gitlabSettings.removeGitlabHostSettings(gitlabHost)
        }
    }

    private inner class AccessTokenLoginPredicate : ComponentPredicate() {

        @RequiresEdt
        override fun addListener(listener: (Boolean) -> Unit) {
            gitlabHostTxtField.document.addDocumentListener(createDocumentChangeListener(listener))
            gitlabAccessTokenTxtField.document.addDocumentListener(createDocumentChangeListener(listener))
        }

        @RequiresEdt
        private fun createDocumentChangeListener(listener: (Boolean) -> Unit): DocumentListener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                listener(invoke())
            }
        }

        @RequiresEdt
        override fun invoke(): Boolean = gitlabHostTxtField.text.isNotBlank() && gitlabAccessTokenTxtField.text.isNotBlank() && !loginSucceeded
    }
}
