package com.github.xeonkryptos.gitlabintegration.services

import com.intellij.openapi.project.Project
import com.github.xeonkryptos.gitlabintegration.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
