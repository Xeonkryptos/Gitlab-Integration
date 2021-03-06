package com.github.xeonkryptos.integration.gitlab.api.gitlab.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import java.awt.Image
import java.net.URL
import javax.imageio.ImageIO

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabUser @JsonCreator constructor(@JsonProperty("id", required = true) val userId: Long,
                                               @JsonProperty("username", required = true) val username: String,
                                               @JsonProperty("name", required = true) val name: String,
                                               @JsonProperty("web_url", required = true) val server: String,
                                               @JsonProperty("avatar_url", required = true) val avatarUrl: String) {

    private companion object {
        private val LOG = GitlabUtil.LOG
    }



    val avatar: Image? = run {
        val convertedUrl = URL(avatarUrl)
        try {
            return@run ImageIO.read(convertedUrl)
        } catch (e: Exception) {
            LOG.warn("Loading of avatar image for user $username at host $server failed", e)
            return@run null
        }
    }
}
