package com.github.xeonkryptos.integration.gitlab.api.gitlab.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.awt.Image
import java.net.URL
import javax.imageio.ImageIO

/**
 * @author Xeonkryptos
 * @since 11.12.2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabUser @JsonCreator constructor(
    @JsonProperty("id", required = true) val userId: Int,
    @JsonProperty("username", required = true) val username: String,
    @JsonProperty("name", required = true) val name: String,
    @JsonProperty("web_url", required = true) val server: String,
    @JsonProperty("avatar_url", required = true) val avatarUrl: String
) {

    val avatar: Image? = run {
        val convertedUrl = URL(avatarUrl)
        return@run ImageIO.read(convertedUrl)
    }
}
