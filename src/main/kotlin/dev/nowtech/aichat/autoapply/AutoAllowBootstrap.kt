package dev.nowtech.aichat.autoapply

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

internal fun bootstrapAutoAllow(project: Project, source: String) {
  if (project.isDisposed) return
  try {
    project.service<AutoAllowProjectService>().start()
    autoAllowLog.warn("AI Chat auto-allow: bootstrap ok ($source, project \"${project.name}\")")
  } catch (t: Throwable) {
    autoAllowLog.warn("AI Chat auto-allow: bootstrap failed ($source, project \"${project.name}\")", t)
  }
}
