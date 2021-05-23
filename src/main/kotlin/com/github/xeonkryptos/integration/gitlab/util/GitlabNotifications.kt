package com.github.xeonkryptos.integration.gitlab.util

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vcs.VcsNotifier
import git4idea.i18n.GitBundle
import org.jetbrains.annotations.NonNls

object GitlabNotifications {

    private val LOG: Logger = GitlabUtil.LOG

    fun showInfo(project: Project, @NonNls displayId: String?, title: String, message: String) {
        LOG.info("$title; $message")
        VcsNotifier.getInstance(project).notifyImportantInfo(displayId, title, message)
    }

    fun showWarning(project: Project, @NonNls displayId: String?, title: String, message: String) {
        LOG.info("$title; $message")
        VcsNotifier.getInstance(project).notifyImportantWarning(displayId, title, message)
    }

    fun showError(project: Project, @NonNls displayId: String?, title: String, message: String) {
        LOG.info("$title; $message")
        VcsNotifier.getInstance(project).notifyError(displayId, title, message)
    }

    fun showError(project: Project, @NonNls displayId: String?, title: String, message: String, logDetails: String) {
        LOG.warn("$title; $message; $logDetails")
        VcsNotifier.getInstance(project).notifyError(displayId, title, message)
    }

    fun showError(project: Project, @NonNls displayId: String?, title: String, e: Throwable) {
        LOG.warn("$title; ", e)
        if (isOperationCanceled(e)) return
        VcsNotifier.getInstance(project).notifyError(displayId, title, GitlabUtil.getErrorTextFromException(e))
    }

    private fun isOperationCanceled(e: Throwable): Boolean = e is ProcessCanceledException

    fun showInfoURL(project: Project, @NonNls displayId: String?, title: String, message: String, url: String) {
        LOG.info("$title; $message; $url")
        VcsNotifier.getInstance(project).notifyImportantInfo(displayId, title, HtmlChunk.link(url, message).toString(), NotificationListener.URL_OPENING_LISTENER)
    }

    fun showWarningURL(project: Project, @NonNls displayId: String?, title: String, prefix: String, highlight: String, postfix: String, url: String) {
        LOG.info("$title; $prefix$highlight$postfix; $url")
        VcsNotifier.getInstance(project).notifyImportantWarning(displayId, title, "$prefix<a href='$url'>$highlight</a>$postfix", NotificationListener.URL_OPENING_LISTENER)
    }

    fun showErrorURL(project: Project, @NonNls displayId: String?, title: String, prefix: String, highlight: String, postfix: String, url: String) {
        LOG.info("$title; $prefix$highlight$postfix; $url")
        VcsNotifier.getInstance(project).notifyError(displayId, title, "$prefix<a href='$url'>$highlight</a>$postfix", NotificationListener.URL_OPENING_LISTENER)
    }

    fun showWarningDialog(project: Project?, title: String, message: String) {
        LOG.info("$title; $message")
        Messages.showWarningDialog(project, message, title)
    }

    fun showErrorDialog(project: Project?, title: String, message: String) {
        LOG.info("$title; $message")
        Messages.showErrorDialog(project, message, title)
    }

    @Messages.YesNoResult
    fun showYesNoDialog(project: Project?, title: String, message: String): Boolean {
        return MessageDialogBuilder.yesNo(title, message).ask(project)
    }

    @Messages.YesNoResult
    fun showYesNoDialog(project: Project?, title: String, message: String, doNotAskOption: DialogWrapper.DoNotAskOption): Boolean {
        return MessageDialogBuilder.yesNo(title, message).icon(Messages.getQuestionIcon()).doNotAsk(doNotAskOption).ask(project)
    }

    fun getConfigureAction(project: Project): AnAction {
        return NotificationAction.createSimple(GitBundle.messagePointer("action.NotificationAction.GithubNotifications.text.configure")) {
            service<ShowSettingsUtil>().showSettingsDialog(project, GitlabUtil.SERVICE_DISPLAY_NAME)
        }
    }
}
