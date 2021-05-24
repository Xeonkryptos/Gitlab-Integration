package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.BrowserLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import git4idea.repo.GitRepositoryManager
import java.awt.BorderLayout
import java.awt.Container
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ProjectLinkerAction : DumbAwareAction(GitlabBundle.message("share.action"), GitlabBundle.message("share.action.description"), GitlabUtil.GITLAB_ICON) {

    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabledAndVisible = project != null && !project.isDefault
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null || project.isDisposed) return
        service<FileDocumentManager>().saveAllDocuments()

        val gitlabSettingsService = service<GitlabSettingsService>()
        val allGitlabAccounts = gitlabSettingsService.state.getAllGitlabAccounts()

        val module: Module? = if (file != null) ModuleUtil.findModuleForFile(file, project) else null

        val projectRootManager = ProjectRootManager.getInstance(project)
        val gitRepositoryManager = project.service<GitRepositoryManager>()
        val repositories = projectRootManager.contentRoots.mapNotNull { gitRepositoryManager.getRepositoryForFileQuick(it) }
        if (repositories.isNotEmpty()) {
            val knownGitlabDomains = allGitlabAccounts.map { it.getGitlabDomain() }
            val knownGitlabRemoteConfigurations = repositories.asSequence().flatMap { it.remotes }.flatMap { remote -> remote.urls }.mapNotNull {
                for (knownGitlabDomain in knownGitlabDomains) {
                    if (it.contains("$knownGitlabDomains/")) return@mapNotNull it
                }
                return@mapNotNull null
            }.toList()
            if (knownGitlabRemoteConfigurations.isNotEmpty()) {
                if (!GitlabExistingRemotesDialog(project, knownGitlabRemoteConfigurations).showAndGet()) return
            }
        }

        val projectLinkerDialog = ProjectLinkerDialog(project, module)
        projectLinkerDialog.fillWithDefaultValues(allGitlabAccounts)
        if (projectLinkerDialog.showAndGet()) {
            val projectLinkerConfiguration = projectLinkerDialog.constructProjectLinkerConfiguration()
            val gitlabHostWithoutProtocol = "${projectLinkerConfiguration.gitlabAccount.getGitlabDomain()}/"
            val foundGitRepository = gitRepositoryManager.getRepositoryForRootQuick(projectLinkerConfiguration.rootDir)
            if (foundGitRepository != null && foundGitRepository.remotes.asSequence().flatMap { it.pushUrls }.any { it.contains(gitlabHostWithoutProtocol) }) {
                UploadGitRepoAndShareTask(project, projectLinkerConfiguration, foundGitRepository).queue()
            } else if (foundGitRepository != null) {
                UploadGitRepoAndShareTask(project, projectLinkerConfiguration, foundGitRepository).queue()
            } else {
                CreateNewGitRepoAndShareTask(project, projectLinkerConfiguration).queue()
            }
        }
    }

    class GitlabExistingRemotesDialog(project: Project, private val remotes: List<String>) : DialogWrapper(project) {

        init {
            title = GitlabBundle.message("share.error.project.is.on.gitlab")
            setOKButtonText(GitlabBundle.message("share.anyway.button"))
            init()
        }

        override fun createCenterPanel(): JComponent {
            val mainText = JBLabel(if (remotes.size == 1) GitlabBundle.message("share.action.remote.is.on.gitlab")
                                   else GitlabBundle.message("share.action.remotes.are.on.gitlab"))

            val remotesPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }
            for (remote in remotes) {
                remotesPanel.add(BrowserLink(remote, remote))
            }

            val messagesPanel = JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP).addToTop(mainText).addToCenter(remotesPanel)

            val iconContainer = Container().apply {
                layout = BorderLayout()
                add(JLabel(Messages.getQuestionIcon()), BorderLayout.NORTH)
            }
            return JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP).addToCenter(messagesPanel).addToLeft(iconContainer).apply { border = JBUI.Borders.emptyBottom(UIUtil.LARGE_VGAP) }
        }
    }
}