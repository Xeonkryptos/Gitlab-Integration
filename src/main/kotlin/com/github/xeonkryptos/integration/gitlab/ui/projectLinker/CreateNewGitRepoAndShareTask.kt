package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.api.gitlab.GitlabProjectsApi
import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabVisibility
import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotificationIdsHolder
import com.github.xeonkryptos.integration.gitlab.util.GitlabNotifications
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.vcsUtil.VcsFileUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.i18n.GitBundle
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import git4idea.util.GitFileUtils
import javax.swing.JComponent

@Suppress("DialogTitleCapitalization")
class CreateNewGitRepoAndShareTask(project: Project,
                                   private val projectName: String,
                                   private val rootDir: VirtualFile,
                                   private val remoteName: String,
                                   private val visibility: GitlabVisibility,
                                   private val projectNamespaceId: Long?,
                                   private val description: String,
                                   private val gitlabAccount: GitlabAccount) : Task.Backgroundable(project, GitlabBundle.message("share.process")) {

    companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val git = service<Git>()
    private val createNewProjectUrl = GitlabProjectsApi.createNewProjectUrl(gitlabAccount).toString()

    override fun run(indicator: ProgressIndicator) {
        val gitRepositoryManager = project.service<GitRepositoryManager>()
        indicator.checkCanceled()

        LOG.info("Creating Gitlab repository")
        indicator.text = GitlabBundle.message("share.process.creating.repository")
        val createdProject = service<GitlabProjectsApi>().createNewProject(projectName, visibility, projectNamespaceId, description, gitlabAccount)
        val remotePushUrl = if (gitlabAccount.useSSH) createdProject.sshUrlToRepo else createdProject.httpUrlToRepo
        LOG.info("Successfully created Gitlab repository")

        LOG.info("Binding local project with Gitlab")
        indicator.text = GitlabBundle.message("share.process.creating.git.repository")
        val initResult = git.init(project, rootDir)
        if (!initResult.success()) {
            project.service<VcsNotifier>().notifyError(GitlabNotificationIdsHolder.GIT_REPO_INIT_REPO, GitBundle.message("initializing.title"), initResult.errorOutputAsHtmlString)
            return
        }
        LOG.info("Adding Gitlab as a remote host")
        indicator.text = GitlabBundle.message("share.process.adding.gitlab.as.remote.host")
        val repository = gitRepositoryManager.getRepositoryForRoot(rootDir)
        if (repository == null) {
            GitlabNotifications.showError(project,
                                          GitlabNotificationIdsHolder.SHARE_CANNOT_FIND_GIT_REPO,
                                          GitlabBundle.message("share.error.failed.to.create.repo"),
                                          GitlabBundle.message("cannot.find.git.repo"))
            return
        }
        git.addRemote(repository, remoteName, remotePushUrl).throwOnError()
        repository.update()

        if (!performFirstCommitIfRequired(rootDir, repository, indicator)) return

        LOG.info("Pushing to gitlab main branch")
        indicator.text = GitlabBundle.message("share.process.pushing.to.gitlab.main")
        if (!pushCurrentBranch(repository, remotePushUrl)) return

        GitlabNotifications.showInfoURL(project,
                                        GitlabNotificationIdsHolder.SHARE_PROJECT_SUCCESSFULLY_SHARED,
                                        GitlabBundle.message("share.process.successfully.shared"),
                                        projectName,
                                        createNewProjectUrl)
    }

    private fun performFirstCommitIfRequired(root: VirtualFile, repository: GitRepository, indicator: ProgressIndicator): Boolean { // check if there is no commits
        if (!repository.isFresh) {
            return true
        }

        LOG.info("Trying to commit")
        try {
            LOG.info("Adding files for commit")
            indicator.text = GitlabBundle.message("share.process.adding.files")

            // ask for files to add
            val trackedFiles = project.service<ChangeListManager>().affectedFiles
            val untrackedFiles = filterOutIgnored(project, repository.untrackedFilesHolder.retrieveUntrackedFilePaths().mapNotNull(FilePath::getVirtualFile))
            trackedFiles.removeAll(untrackedFiles) // fix IDEA-119855

            val allFiles = ArrayList<VirtualFile>()
            allFiles.addAll(trackedFiles)
            allFiles.addAll(untrackedFiles)

            val dialog = invokeAndWaitIfNeeded(indicator.modalityState) {
                GitlabUntrackedFilesDialog(allFiles).apply {
                    if (trackedFiles.isNotEmpty()) selectedFiles = trackedFiles
                    show()
                }
            }

            val files2commit = dialog.selectedFiles
            if (!dialog.isOK || files2commit.isEmpty()) {
                GitlabNotifications.showInfoURL(project,
                                                GitlabNotificationIdsHolder.SHARE_EMPTY_REPO_CREATED,
                                                GitlabBundle.message("share.process.empty.project.created"),
                                                projectName,
                                                createNewProjectUrl)
                return false
            }

            val files2add = ContainerUtil.intersection(untrackedFiles, files2commit)
            val files2rm = ContainerUtil.subtract(trackedFiles, files2commit)
            val modified = HashSet(trackedFiles)
            modified.addAll(files2commit)

            GitFileUtils.addFiles(project, root, files2add)
            GitFileUtils.deleteFilesFromCache(project, root, files2rm)

            // commit
            LOG.info("Performing commit")
            indicator.text = GitlabBundle.message("share.process.performing.commit")
            val handler = GitLineHandler(project, root, GitCommand.COMMIT)
            handler.setStdoutSuppressed(false)
            handler.addParameters("-m", dialog.commitMessage)
            handler.endOptions()
            git.runCommand(handler).throwOnError()

            VcsFileUtil.markFilesDirty(project, modified)
        } catch (e: VcsException) {
            LOG.warn(e)
            GitlabNotifications.showErrorURL(project,
                                             GitlabNotificationIdsHolder.SHARE_PROJECT_INIT_COMMIT_FAILED,
                                             GitlabBundle.message("share.error.cannot.finish"),
                                             GitlabBundle.message("share.error.created.project"),
                                             " '$projectName' ",
                                             GitlabBundle.message("share.error.init.commit.failed") + GitlabUtil.getErrorTextFromException(e),
                                             createNewProjectUrl)
            return false
        }

        LOG.info("Successfully created initial commit")
        return true
    }

    private fun filterOutIgnored(project: Project, files: Collection<VirtualFile>): Collection<VirtualFile> {
        val changeListManager = ChangeListManager.getInstance(project)
        val vcsManager = ProjectLevelVcsManager.getInstance(project)
        return ContainerUtil.filter(files) { file -> !changeListManager.isIgnoredFile(file) && !vcsManager.isIgnored(file) }
    }

    private fun pushCurrentBranch(repository: GitRepository, remoteUrl: String): Boolean {
        val currentBranch = repository.currentBranch
        if (currentBranch == null) {
            GitlabNotifications.showErrorURL(project,
                                             GitlabNotificationIdsHolder.SHARE_PROJECT_INIT_PUSH_FAILED,
                                             GitlabBundle.message("share.error.cannot.finish"),
                                             GitlabBundle.message("share.error.created.project"),
                                             " '$projectName' ",
                                             GitlabBundle.message("share.error.push.no.current.branch"),
                                             createNewProjectUrl)
            return false
        }
        val result = git.push(repository, remoteName, remoteUrl, currentBranch.name, true)
        if (!result.success()) {
            GitlabNotifications.showErrorURL(project,
                                             GitlabNotificationIdsHolder.SHARE_PROJECT_INIT_PUSH_FAILED,
                                             GitlabBundle.message("share.error.cannot.finish"),
                                             GitlabBundle.message("share.error.created.project"),
                                             " '$projectName' ",
                                             GitlabBundle.message("share.error.push.failed", result.errorOutputAsHtmlString),
                                             createNewProjectUrl)
            return false
        }
        return true
    }

    override fun onThrowable(error: Throwable) {
        GitlabNotifications.showError(project, GitlabNotificationIdsHolder.SHARE_CANNOT_CREATE_REPO, GitlabBundle.message("share.error.failed.to.create.repo"), error)
    }

    private inner class GitlabUntrackedFilesDialog(untrackedFiles: List<VirtualFile>) : SelectFilesDialog(project, untrackedFiles, null, null, true, false), DataProvider {
        private var commitMessagePanel: CommitMessage? = null

        val commitMessage: String
            get() = commitMessagePanel!!.comment

        init {
            title = GitlabBundle.message("untracked.files.dialog.title")
            setOKButtonText(CommonBundle.getAddButtonText())
            setCancelButtonText(CommonBundle.getCancelButtonText())
            init()
        }

        override fun createNorthPanel(): JComponent? = null

        override fun createCenterPanel(): JComponent {
            val tree = super.createCenterPanel()

            val commitMessage = CommitMessage(project)
            Disposer.register(disposable, commitMessage)
            commitMessage.setCommitMessage("Initial commit")
            commitMessagePanel = commitMessage

            val splitter = Splitter(true)
            splitter.setHonorComponentsMinimumSize(true)
            splitter.firstComponent = tree
            splitter.secondComponent = commitMessagePanel
            splitter.proportion = 0.7f

            return splitter
        }

        override fun getData(dataId: String): Any? = if (VcsDataKeys.COMMIT_MESSAGE_CONTROL.`is`(dataId)) commitMessagePanel else null

        override fun getDimensionServiceKey(): String = "Gitlab.UntrackedFilesDialog"
    }
}