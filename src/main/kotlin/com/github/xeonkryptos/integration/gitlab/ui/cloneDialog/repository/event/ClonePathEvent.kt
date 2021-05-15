package com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.repository.event

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import java.util.*

class ClonePathEvent(source: Any, val gitlabProject: GitlabProject?) : EventObject(source)