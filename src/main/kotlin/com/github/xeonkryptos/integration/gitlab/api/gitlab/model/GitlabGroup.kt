package com.github.xeonkryptos.integration.gitlab.api.gitlab.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.util.ui.cloneDialog.SearchableListItem

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabGroup @JsonCreator constructor(@JsonProperty("id", required = true) val id: Long,
                                                @JsonProperty("full_name", required = true) val fullName: String,
                                                @JsonProperty("avatar_url") val avatarUrl: String?) : SearchableListItem {

    override val stringToSearch: String
        get() = fullName
}
