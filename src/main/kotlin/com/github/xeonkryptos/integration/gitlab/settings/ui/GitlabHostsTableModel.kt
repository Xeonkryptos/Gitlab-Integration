package com.github.xeonkryptos.integration.gitlab.settings.ui

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabSettings
import javax.swing.table.AbstractTableModel
import org.jetbrains.annotations.Nls

/**
 * @author Xeonkryptos
 * @since 16.02.2021
 */
class GitlabHostsTableModel(private val settings: GitlabSettings) : AbstractTableModel() {

    private val mutableGitlabHostSettings: MutableList<GitlabHostSettings>
    val gitlabHostSettings: List<GitlabHostSettings>
        get() = mutableGitlabHostSettings

    init {
        val localGitlabHostSettings: Collection<GitlabHostSettings> = settings.gitlabHostSettings.values
        mutableGitlabHostSettings = mutableListOf()
        for (localGitlabHostSetting in localGitlabHostSettings) {
            mutableGitlabHostSettings.add(localGitlabHostSetting.copy())
        }
    }

    override fun getRowCount(): Int {
        return mutableGitlabHostSettings.size
    }

    override fun getColumnCount(): Int = 2

    override fun getColumnName(columnIndex: Int): @Nls String? {
        when (columnIndex) {
            0 -> return "Gitlab Host"
            1 -> return "Disable certificate verification"
        }
        return null
    }

    override fun getColumnClass(columnIndex: Int): Class<*>? {
        return when (columnIndex) {
            0    -> String::class.java
            1    -> Boolean::class.java
            else -> null
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        val mutableGitlabHostSettings = mutableGitlabHostSettings[rowIndex]
        return if (mutableGitlabHostSettings.gitlabHost != "https://gitlab.com/") columnIndex > 0
        else false
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val gitlabHostSettings = mutableGitlabHostSettings[rowIndex]
        when (columnIndex) {
            0 -> return gitlabHostSettings.gitlabHost
            1 -> return gitlabHostSettings.disableSslVerification
        }
        return null
    }

    override fun setValueAt(value: Any, rowIndex: Int, columnIndex: Int) {
        val localGitlabHostSettings = mutableGitlabHostSettings[rowIndex]
        if (localGitlabHostSettings.gitlabHost != "https://gitlab.com/") {
            if (columnIndex == 1) {
                localGitlabHostSettings.disableSslVerification = (value as Boolean)
            }
        }
    }

    fun apply() {
        gitlabHostSettings.forEach { settings.gitlabHostSettings[it.gitlabHost]?.updateWith(it) }
    }

    fun reset() {
        gitlabHostSettings.forEach { it.updateWith(settings.gitlabHostSettings[it.gitlabHost]!!) }
    }
}
