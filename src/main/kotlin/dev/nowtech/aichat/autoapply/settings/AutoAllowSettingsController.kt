package dev.nowtech.aichat.autoapply.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import dev.nowtech.aichat.autoapply.AutoAllowProjectService
import dev.nowtech.aichat.autoapply.autoAllowLog

object AutoAllowSettingsController {
  fun isEnabled(): Boolean = AutoAllowSettings.getInstance().enabled

  fun setEnabled(enabled: Boolean) {
    val settings = AutoAllowSettings.getInstance()
    if (settings.enabled == enabled) return
    settings.enabled = enabled
    applyToOpenProjects(enabled)
    autoAllowLog.warn("AI Chat auto-allow: ${if (enabled) "enabled" else "disabled"} (user toggle)")
  }

  fun applyToOpenProjects(enabled: Boolean = settings.enabled) {
    for (project in ProjectManager.getInstance().openProjects) {
      val service = project.service<AutoAllowProjectService>()
      if (enabled) service.restart() else service.stop()
    }
  }

  private val settings: AutoAllowSettings
    get() = AutoAllowSettings.getInstance()
}
