package dev.nowtech.aichat.autoapply

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.nowtech.aichat.autoapply.settings.AutoAllowSettingsController
import com.intellij.openapi.actionSystem.ToggleAction

class ToggleAutoAllowAction : ToggleAction(), DumbAware {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun isSelected(e: AnActionEvent): Boolean = AutoAllowSettingsController.isEnabled()

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    AutoAllowSettingsController.setEnabled(state)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    val on = AutoAllowSettingsController.isEnabled()
    e.presentation.isEnabled = true
    e.presentation.text = if (on) "AI Chat Auto Allow: On" else "AI Chat Auto Allow: Off"
    e.presentation.description =
      if (on) {
        "Automatic Allow always is active for AI Chat permission prompts"
      } else {
        "Click to enable automatic Allow always in AI Chat"
      }
  }
}
