package com.github.xeonkryptos.integration.gitlab.api.gitlab.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabGroup @JsonCreator constructor(
    @JsonProperty("id", required = true) val id: Long, @JsonProperty("full_name", required = true) val fullName: String
)
