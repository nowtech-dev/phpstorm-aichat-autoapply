package dev.nowtech.aichat.autoapply

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AutoAllowProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    bootstrapAutoAllow(project, "projectActivity")
  }
}
