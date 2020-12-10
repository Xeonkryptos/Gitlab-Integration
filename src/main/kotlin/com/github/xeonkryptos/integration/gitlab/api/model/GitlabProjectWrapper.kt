package com.github.xeonkryptos.integration.gitlab.api.model

import org.gitlab4j.api.models.Project

/**
 * A simple wrapper around [Project] of gitlab4j api to avoid confusions with the IntelliJ SDK project pendant [com.intellij.openapi.project.Project]. Simply use this project's get method/property
 * syntax to access its content rather than safe it unwrapped in its own value
 *
 * @author Xeonkryptos
 * @since 17.09.2020
 */
data class GitlabProjectWrapper(val project: Project)