package dev.nowtech.aichat.autoapply.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class AutoAllowConfigurable : Configurable {

  private var enabledBox: JBCheckBox? = null
  private var terminalOnlyBox: JBCheckBox? = null

  override fun getDisplayName(): String = "AI Chat Auto Allow"

  override fun createComponent(): JComponent {
    val settings = AutoAllowSettings.getInstance()
    enabledBox = JBCheckBox("Enable automatic approval (prefer Allow always)", settings.enabled)
    terminalOnlyBox =
      JBCheckBox(
        "Limit to terminal runs and agent permission prompts (e.g. Cursor MCP in AI Chat)",
        settings.terminalCommandsOnly,
      )
    return FormBuilder.createFormBuilder()
      .addComponent(enabledBox!!)
      .addComponent(terminalOnlyBox!!)
      .panel
  }

  override fun isModified(): Boolean {
    val settings = AutoAllowSettings.getInstance()
    return enabledBox?.isSelected != settings.enabled ||
      terminalOnlyBox?.isSelected != settings.terminalCommandsOnly
  }

  override fun apply() {
    val settings = AutoAllowSettings.getInstance()
    settings.terminalCommandsOnly = terminalOnlyBox?.isSelected == true
    AutoAllowSettingsController.setEnabled(enabledBox?.isSelected == true)
    if (AutoAllowSettingsController.isEnabled()) {
      AutoAllowSettingsController.applyToOpenProjects(true)
    }
  }

  override fun reset() {
    val settings = AutoAllowSettings.getInstance()
    enabledBox?.isSelected = settings.enabled
    terminalOnlyBox?.isSelected = settings.terminalCommandsOnly
  }
}
