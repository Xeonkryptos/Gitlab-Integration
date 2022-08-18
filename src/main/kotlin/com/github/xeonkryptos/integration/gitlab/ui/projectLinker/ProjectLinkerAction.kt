package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.util.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.BrowserLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import git4idea.repo.GitRepositoryManager
import java.awt.BorderLayout
import java.awt.Container
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import kotlin.math.min

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
        val allGitlabAccounts = gitlabSettingsService.getWorkableState().getAllGitlabAccounts()

        val module: Module? = if (file != null) ModuleUtil.findModuleForFile(file, project) else null
        val projectFileIndex: ProjectFileIndex = project.service()

        val projectRootManager = ProjectRootManager.getInstance(project)
        val gitRepositoryManager = project.service<GitRepositoryManager>()
        val repositories = projectRootManager.contentRoots.mapNotNull { gitRepositoryManager.getRepositoryForFileQuick(it) }
        if (repositories.isNotEmpty()) {
            val knownGitlabDomains = allGitlabAccounts.map { it.getGitlabDomainWithoutPort() }
            val knownGitlabRemoteConfigurations =
                repositories.asSequence().filter { module == null || module == projectFileIndex.getModuleForFile(it.root) }.flatMap { it.remotes }.flatMap { remote -> remote.urls }.mapNotNull {
                    val gitRemoteHost = GitlabUtil.getGitlabDomainWithoutPort(it)
                    for (knownGitlabDomain in knownGitlabDomains) {
                        if (gitRemoteHost == knownGitlabDomain) return@mapNotNull it
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

    private class GitlabExistingRemotesDialog(project: Project, private val remotes: List<String>) : DialogWrapper(project) {

        companion object {
            private const val MAX_REMOTE_LINKS_AT_ONCE: Int = 5
        }

        init {
            title = GitlabBundle.message("share.error.project.is.on.gitlab")
            setOKButtonText(GitlabBundle.message("share.anyway.button"))
            init()
        }

        override fun createCenterPanel(): JComponent {
            val mainText = JBLabel(if (remotes.size == 1) GitlabBundle.message("share.action.remote.is.on.gitlab") else GitlabBundle.message("share.action.remotes.are.on.gitlab"))

            val remotesPanel = object : ScrollablePanel() {
                override fun getPreferredScrollableViewportSize(): Dimension {
                    val maxRemotesAtOnce = min(remotes.size, MAX_REMOTE_LINKS_AT_ONCE)
                    // Using getScrollableUnitIncrement() because the base class ScrollablePane keeps track of the font metrics to compute the height of the visible text. Just making use of the already
                    // stored data and restrict the number of elements to see at max at once.
                    val preferredHeight = getScrollableUnitIncrement(null, SwingConstants.VERTICAL, 0) * maxRemotesAtOnce
                    return Dimension(preferredSize.width, preferredHeight)
                }
            }.apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }
            remotes.forEach { remotesPanel.add(BrowserLink(it, it)) }

            val remotesScrollPane = JBScrollPane(remotesPanel).apply {
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                border = EmptyBorder(0, 0, 0, 0)
            }
            val messagesPanel = JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP).addToTop(mainText).addToCenter(remotesScrollPane)

            val iconContainer = Container().apply {
                layout = BorderLayout()
                add(JLabel(Messages.getQuestionIcon()), BorderLayout.NORTH)
            }
            return JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP).addToCenter(messagesPanel).addToLeft(iconContainer).apply { border = JBUI.Borders.emptyBottom(UIUtil.LARGE_VGAP) }
        }
    }
}