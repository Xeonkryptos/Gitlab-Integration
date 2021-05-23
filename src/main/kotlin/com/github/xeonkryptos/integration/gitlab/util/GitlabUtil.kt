package com.github.xeonkryptos.integration.gitlab.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.text.StringUtil
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
}
