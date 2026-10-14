package dev.nowtech.aichat.autoapply

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class AutoAllowProjectListener : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    bootstrapAutoAllow(project, "projectOpened")
  }
}
