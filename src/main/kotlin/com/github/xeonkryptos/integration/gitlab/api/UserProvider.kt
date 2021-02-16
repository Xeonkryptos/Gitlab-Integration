package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
interface UserProvider {

    fun getUsers(): Map<GitlabAccount, GitlabUser>
}
