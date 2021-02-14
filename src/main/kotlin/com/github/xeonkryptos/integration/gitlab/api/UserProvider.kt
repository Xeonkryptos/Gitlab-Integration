package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
interface UserProvider {

    fun getUsers(): List<GitlabUser>
}
