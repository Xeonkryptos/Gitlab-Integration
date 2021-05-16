package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog

data class GitlabLoginData(
    val gitlabHost: String, val gitlabAccessToken: String, val disableCertificateValidation: Boolean
)
