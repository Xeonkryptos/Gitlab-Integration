package com.github.xeonkryptos.integration.gitlab.ui.projectLinker

import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabVisibility
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.intellij.openapi.vfs.VirtualFile

data class ProjectLinkingConfiguration(val projectName: String,
                                       val rootDir: VirtualFile,
                                       val remoteName: String,
                                       val visibility: GitlabVisibility,
                                       val projectNamespaceId: Long?,
                                       val description: String,
                                       val gitlabAccount: GitlabAccount)
