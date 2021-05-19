package com.github.xeonkryptos.integration.gitlab.settings.ui

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabAccountStateNotifier
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import javax.swing.table.AbstractTableModel

/**
 * Requests against the main gitlab project page: gitlab.com are only allowed for own projects. Loading of ALL available public repositories task a reaaaally long time to finish. You usually don't want
 * that. Also, gitlab.com is ever growing. Therefore, resolving against gitlab.com is restricted. When someone really wants to clone a public repository, they should do it with the provided simple clone
 * functionality of their Jetbrains IDE itself.
 *
 * @author Xeonkryptos
 * @since 16.02.2021
 */
class GitlabHostsTableModel(private val originalSettings: GitlabSettings) : AbstractTableModel(), Disposable {

    private val currentSettings: GitlabSettings = originalSettings.deepCopy()

    private val flattenedSettings: MutableList<Any> = mutableListOf()

    init {
        rebuildFlattenedSettings()

        val applicationManager = ApplicationManager.getApplication()
        val messageBusConnection = applicationManager.messageBus.connect(this)
        messageBusConnection.subscribe(GitlabAccountStateNotifier.ACCOUNT_STATE_TOPIC, object : GitlabAccountStateNotifier {
            override fun onGitlabAccountCreated(gitlabAccount: GitlabAccount): Unit = applicationManager.invokeLater { registerNewGitlabAccount(gitlabAccount) }

            override fun onGitlabAccountDeleted(gitlabAccount: GitlabAccount): Unit = applicationManager.invokeLater { removeGitlabAccount(gitlabAccount) }
        })
    }

    override fun getColumnCount(): Int = 4

    override fun getColumnName(column: Int): String? {
        return when (column) {
            0 -> GitlabBundle.message("settings.general.table.column.host")
            1 -> GitlabBundle.message("settings.general.table.column.certificates")
            2 -> GitlabBundle.message("settings.general.table.column.username")
            3 -> GitlabBundle.message("settings.general.table.column.resolve")
            else -> null
        }
    }

    override fun getColumnClass(column: Int): Class<*>? {
        return when (column) {
            0 -> String::class.java
            1 -> Boolean::class.java
            2 -> String::class.java
            3 -> Boolean::class.java
            else -> null
        }
    }

    override fun getRowCount(): Int = flattenedSettings.size

    override fun getValueAt(row: Int, column: Int): Any? {
        val userObject = flattenedSettings[row]
        if (userObject is GitlabHostSettings) {
            return when (column) {
                0 -> userObject.gitlabHost
                1 -> userObject.disableSslVerification
                else -> null
            }
        }
        if (userObject is GitlabAccount) {
            return when (column) {
                2 -> userObject.username
                3 -> userObject.resolveOnlyOwnProjects
                else -> null
            }
        }
        return null
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        val userObject = flattenedSettings[row]
        if (userObject is GitlabHostSettings && column == 1) {
            return true
        }
        return userObject is GitlabAccount && column == 3
    }

    override fun setValueAt(aValue: Any?, row: Int, column: Int) {
        val userObject = flattenedSettings[row]
        if (userObject is GitlabHostSettings && column == 1 && aValue is Boolean) {
            userObject.disableSslVerification = aValue
        } else if (userObject is GitlabAccount && column == 3 && aValue is Boolean) {
            userObject.resolveOnlyOwnProjects = aValue
        }
    }

    private fun registerNewGitlabAccount(createdGitlabAccount: GitlabAccount) {
        val gitlabHost: String = createdGitlabAccount.getGitlabHost()
        if (!currentSettings.containsGitlabHostSettings(gitlabHost)) {
            val currentGitlabHostSettings = currentSettings.getOrCreateGitlabHostSettings(gitlabHost)
            val originalGitlabHostSetting = originalSettings.gitlabHostSettings[gitlabHost]
            currentGitlabHostSettings.updateWith(originalGitlabHostSetting!!)
        } else {
            val currentGitlabHostSettings = currentSettings.gitlabHostSettings[gitlabHost]
            val currentGitlabAccount = currentGitlabHostSettings?.createGitlabAccount(createdGitlabAccount.username, true)
            currentGitlabAccount?.updateWith(createdGitlabAccount)
        }
        rebuildFlattenedSettings()
    }

    private fun removeGitlabAccount(gitlabAccount: GitlabAccount) {
        val gitlabHost: String = gitlabAccount.getGitlabHost()
        val currentGitlabHostSettings = currentSettings.gitlabHostSettings[gitlabHost]
        if (currentGitlabHostSettings != null) {
            currentGitlabHostSettings.removeGitlabAccount(gitlabAccount, true)
            flattenedSettings.remove(gitlabAccount)
            if (currentGitlabHostSettings.gitlabAccounts.isEmpty()) {
                flattenedSettings.remove(currentGitlabHostSettings)
            }
        }
        fireTableDataChanged()
    }

    fun removeEntry(selectedRow: Int) {
        val removedEntry = flattenedSettings.removeAt(selectedRow)
        var lastDeletedRowIndex: Int = selectedRow
        if (removedEntry is GitlabHostSettings) {
            val size = flattenedSettings.size + 1

            while (selectedRow < flattenedSettings.size && flattenedSettings[selectedRow] is GitlabAccount) (flattenedSettings.removeAt(selectedRow) as GitlabAccount).delete()
            currentSettings.removeGitlabHostSettings(removedEntry.gitlabHost)

            lastDeletedRowIndex = size - flattenedSettings.size
        } else if (removedEntry is GitlabAccount) {
            removedEntry.delete()
        }
        fireTableRowsDeleted(selectedRow, lastDeletedRowIndex)
    }

    fun apply() {
        currentSettings.gitlabHostSettings.values.forEach { originalSettings.gitlabHostSettings[it.gitlabHost]?.updateWith(it) }
        originalSettings.gitlabHostSettings.keys.filterNot { currentSettings.containsGitlabHostSettings(it) }.forEach { originalSettings.removeGitlabHostSettings(it) }
    }

    fun isModified(): Boolean = originalSettings.isModified(currentSettings)

    fun reset() {
        currentSettings.gitlabHostSettings.values.forEach { it.updateWith(originalSettings.gitlabHostSettings[it.gitlabHost]!!) }
        originalSettings.gitlabHostSettings.filterNot { entry -> currentSettings.containsGitlabHostSettings(entry.key) }
            .forEach { entry -> currentSettings.getOrCreateGitlabHostSettings(entry.key).updateWith(entry.value) }

        flattenedSettings.clear()
        rebuildFlattenedSettings()

        fireTableDataChanged()
    }

    private fun rebuildFlattenedSettings() {
        currentSettings.gitlabHostSettings.values.forEach { gitlabHostSettings ->
            flattenedSettings.add(gitlabHostSettings)
            gitlabHostSettings.gitlabAccounts.forEach { gitlabAccount -> flattenedSettings.add(gitlabAccount) }
        }
    }

    override fun dispose() {}
}
