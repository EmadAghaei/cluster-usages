package com.github.eaghayi.clusterusages.services

import com.github.eaghayi.clusterusages.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
