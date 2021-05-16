package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ProjectLinkerAction: AnAction() {

    override fun update(e: AnActionEvent) {
        // TODO: Only enabled when at least one module is selected or there is only one module to be used
    }

    override fun actionPerformed(e: AnActionEvent) {
        TODO("Open dialog to define the target gitlab server, account and path")
    }
}