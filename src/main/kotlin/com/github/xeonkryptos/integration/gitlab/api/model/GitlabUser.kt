package com.github.xeonkryptos.integration.gitlab.api.model

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import java.awt.Image
import java.net.URL
import javax.imageio.ImageIO
import org.gitlab4j.api.models.User

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
class GitlabUser(user: User, val gitlabAccount: GitlabAccount) {

    val username: String = user.username

    val name: String? = user.name

    val server: String = user.webUrl

    val avatar: Image? = run {
        val convertedUrl = URL(user.avatarUrl)
        return@run ImageIO.read(convertedUrl)
    }
}
