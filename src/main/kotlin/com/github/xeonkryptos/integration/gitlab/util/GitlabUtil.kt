package com.github.xeonkryptos.integration.gitlab.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.IconLoader

/**
 * @author Xeonkryptos
 * @since 06.09.2020
 */
object GitlabUtil {

    @JvmField
    val LOG: Logger = Logger.getInstance("gitlab")

    const val GITLAB_ICON_PATH = "/icons/gitlab-icon-rgb.svg"
    @JvmField
    val GITLAB_ICON = IconLoader.getIcon(GITLAB_ICON_PATH, GitlabUtil::class.java)
}
