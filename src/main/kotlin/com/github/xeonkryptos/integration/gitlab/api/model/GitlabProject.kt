package com.github.xeonkryptos.integration.gitlab.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class GitlabProject @JsonCreator constructor(@JsonProperty("name_with_namespace", required = true) nameWithNameSpace: String, @JsonProperty("http_url_to_repo") httpUrlToRepo: String?) {

    val viewableProjectPath: String = nameWithNameSpace.replace(" / ", "/")
    val httpProjectUrl: String? = httpUrlToRepo
}
