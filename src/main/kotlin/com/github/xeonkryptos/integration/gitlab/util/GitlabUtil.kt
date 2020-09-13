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
    @JvmField
    val GITLAB_ICON = IconLoader.getIcon("/icons/gitlab-icon-rgb.svg")
}
