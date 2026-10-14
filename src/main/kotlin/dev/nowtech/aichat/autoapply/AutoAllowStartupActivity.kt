package dev.nowtech.aichat.autoapply

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/** Runs after project open (post-startup); complements [AutoAllowProjectActivity]. */
class AutoAllowStartupActivity : StartupActivity.DumbAware {
  override fun runActivity(project: Project) {
    bootstrapAutoAllow(project, "postStartupActivity")
  }
}
