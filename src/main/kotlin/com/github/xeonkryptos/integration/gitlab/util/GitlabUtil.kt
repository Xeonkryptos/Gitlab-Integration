package com.github.xeonkryptos.integration.gitlab.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.text.StringUtil
import java.net.URI
import java.net.UnknownHostException

/**
 * @author Xeonkryptos
 * @since 06.09.2020
 */
object GitlabUtil {

    @JvmField
    val LOG: Logger = Logger.getInstance("gitlab")

    const val GITLAB_ICON_PATH = "/icons/gitlab-icon-rgb.svg"
    const val SERVICE_DISPLAY_NAME: String = "Gitlab"

    @JvmField
    val GITLAB_ICON = IconLoader.getIcon(GITLAB_ICON_PATH, GitlabUtil::class.java)

    @JvmStatic
    fun getErrorTextFromException(e: Throwable): String = if (e is UnknownHostException) "Unknown host: " + e.message else StringUtil.notNullize(e.message, "Unknown error")

    @JvmStatic
    fun getGitlabDomain(uriString: String): String {
        val uri = convertToRepoUri(uriString)
        if (uri.scheme == "ssh") {
            val startIndexOfPath = uri.authority.indexOf(':')
            return uri.authority.substring("git@".length, startIndexOfPath)
        }
        return if (uri.port != -1) "${uri.host}:${uri.port}" else uri.host
    }

    @JvmStatic
    fun getGitlabDomainWithoutPort(uriString: String): String {
        val uri = convertToRepoUri(uriString)
        if (uri.scheme == "ssh") {
            val startIndexOfPath = uri.authority.indexOf(':')
            return uri.authority.substring("git@".length, startIndexOfPath)
        }
        return uri.host
    }

    @JvmStatic
    fun convertToRepoUri(uriString: String): URI = try {
        URI(uriString)
    } catch (e: Exception) {
        // Typically, the ssh clone path doesn't start with the ssh protocol definition, leading to an invalid URI definition. With simply adding it, the URI creation works like a charm. If it still
        // doesn't work, then it simply isn't a valid URI
        URI("ssh://$uriString")
    }
}
