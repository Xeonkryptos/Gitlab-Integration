package com.github.xeonkryptos.integration.gitlab.service

import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabHostStateNotifier
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.service.data.SerializableGitlabSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import javax.swing.SwingUtilities

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
@State(name = "gitlab-settings", storages = [Storage(value = "gitlab.xml")], reportStatistic = false, reloadable = true)
class GitlabSettingsService : PersistentStateComponent<SerializableGitlabSettings> {

    private var gitlabData = SerializableGitlabSettings()

    override fun initializeComponent() {
        val connection = ApplicationManager.getApplication().messageBus.connect()
        connection.subscribe(GitlabHostStateNotifier.HOST_STATE_TOPIC, object : GitlabHostStateNotifier {
            override fun onGitlabHostsWithoutAnyAccounts(gitlabHostSettings: GitlabHostSettings) {
                SwingUtilities.invokeLater { gitlabData.gitlabSettings.removeGitlabHostSettings(gitlabHostSettings.hostSettings.gitlabHost) }
            }
        })
    }

    fun getWorkableState() = gitlabData.gitlabSettings

    override fun getState(): SerializableGitlabSettings = gitlabData

    override fun loadState(state: SerializableGitlabSettings) {
        gitlabData = state
    }
}
