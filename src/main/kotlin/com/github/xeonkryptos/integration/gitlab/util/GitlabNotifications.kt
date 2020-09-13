package com.github.xeonkryptos.integration.gitlab.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier

object GitlabNotifications {

    private val LOG = GitlabUtil.LOG

    @JvmStatic
    fun showError(project: Project, title: String, message: String) {
        LOG.info("$title; $message")
        VcsNotifier.getInstance(project).notifyError(title, message)
    }
}
