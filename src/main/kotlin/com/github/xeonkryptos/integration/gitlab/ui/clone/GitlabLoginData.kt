package com.github.xeonkryptos.integration.gitlab.ui.clone

data class GitlabLoginData(
    val gitlabHost: String, val gitlabAccessToken: String, val disableCertificateValidation: Boolean
)
