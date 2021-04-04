package com.github.xeonkryptos.integration.gitlab.settings.ui

import com.github.xeonkryptos.integration.gitlab.bundle.GitlabBundle
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings
import javax.swing.table.AbstractTableModel

/**
 * Requests against the main gitlab project page: gitlab.com are only allowed for own projects. Loading of ALL available public repositories task a reaaaally long time to finish. You usually don't want
 * that. Also, gitlab.com is ever growing. Therefore, resolving against gitlab.com is restricted. When someone really wants to clone a public repository, they should do it with the provided simple clone
 * functionality of their Jetbrains IDE itself.
 *
 * @author Xeonkryptos
 * @since 16.02.2021
 */
class GitlabHostsTableModel(private val originalSettings: GitlabSettings) : AbstractTableModel() {

    private val currentSettings = originalSettings.deepCopy()

    private val flattenedSettings: MutableList<Any> = mutableListOf()

    init {
        currentSettings.gitlabHostSettings.values.forEach { gitlabHostSettings ->
            flattenedSettings.add(gitlabHostSettings)
            gitlabHostSettings.gitlabAccounts.forEach { gitlabAccount -> flattenedSettings.add(gitlabAccount) }
        }
    }

    override fun getColumnCount(): Int = 4

    override fun getColumnName(column: Int): String? {
        return when (column) {
            0    -> GitlabBundle.message("settings.general.table.column.host")
            1    -> GitlabBundle.message("settings.general.table.column.certificates")
            2    -> GitlabBundle.message("settings.general.table.column.username")
            3    -> GitlabBundle.message("settings.general.table.column.resolve")
            else -> null
        }
    }

    override fun getColumnClass(column: Int): Class<*>? {
        return when (column) {
            0    -> String::class.java
            1    -> Boolean::class.java
            2    -> String::class.java
            3    -> Boolean::class.java
            else -> null
        }
    }

    override fun getRowCount(): Int = flattenedSettings.size

    override fun getValueAt(row: Int, column: Int): Any? {
        val userObject = flattenedSettings[row]
        if (userObject is GitlabHostSettings) {
            return when (column) {
                0    -> userObject.gitlabHost
                1    -> userObject.disableSslVerification
                else -> null
            }
        }
        if (userObject is GitlabAccount) {
            return when (column) {
                2    -> userObject.username
                3    -> userObject.resolveOnlyOwnProjects
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

    fun apply() {
        currentSettings.gitlabHostSettings.values.forEach { originalSettings.gitlabHostSettings[it.gitlabHost]?.updateWith(it) }
    }

    fun isModified(): Boolean = originalSettings.isModified(currentSettings)

    fun reset() {
        currentSettings.gitlabHostSettings.values.forEach { it.updateWith(originalSettings.gitlabHostSettings[it.gitlabHost]!!) }
    }
}
