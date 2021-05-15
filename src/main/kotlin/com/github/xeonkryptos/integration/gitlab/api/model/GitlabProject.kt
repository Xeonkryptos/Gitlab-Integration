package com.github.xeonkryptos.integration.gitlab.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class GitlabProject @JsonCreator constructor(
    @JsonProperty("id", required = true) val id: Int,
    @JsonProperty("name_with_namespace", required = true) nameWithNameSpace: String,
    @JsonProperty("http_url_to_repo") httpUrlToRepo: String?,
    @JsonProperty("description") description: String?
) {

    val viewableProjectPath: String = nameWithNameSpace.replace(" / ", "/")
    val httpProjectUrl: String? = httpUrlToRepo
    val description: String? = description

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GitlabProject) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}
