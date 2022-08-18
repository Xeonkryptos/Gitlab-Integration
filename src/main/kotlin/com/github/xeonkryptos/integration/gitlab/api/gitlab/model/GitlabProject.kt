package com.github.xeonkryptos.integration.gitlab.api.gitlab.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class GitlabProject @JsonCreator constructor(
    @JsonProperty("id", required = true) val id: Int,
    @JsonProperty("name_with_namespace", required = true) nameWithNameSpace: String,
    @JsonProperty("ssh_url_to_repo") val sshUrlToRepo: String,
    @JsonProperty("http_url_to_repo") val httpUrlToRepo: String,
    @JsonProperty("description") val description: String?
) {

    val viewableProjectPath: String = nameWithNameSpace.replace(" / ", "/")

    lateinit var gitlabAccount: GitlabAccount
        internal set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GitlabProject) return false

        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id
}
