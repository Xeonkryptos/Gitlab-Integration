package com.github.xeonkryptos.integration.gitlab.ui.clone.repository.event

import com.github.xeonkryptos.integration.gitlab.api.gitlab.model.GitlabProject
import java.util.*

class ClonePathEvent(source: Any, val gitlabProject: GitlabProject?) : EventObject(source)