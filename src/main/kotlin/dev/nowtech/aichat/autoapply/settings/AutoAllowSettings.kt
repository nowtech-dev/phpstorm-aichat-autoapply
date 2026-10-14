package dev.nowtech.aichat.autoapply.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "AiChatAutoAllowSettings", storages = [Storage("aiChatAutoAllow.xml")])
class AutoAllowSettings : PersistentStateComponent<AutoAllowSettings.State> {

  data class State(
    var enabled: Boolean = true,
    var terminalCommandsOnly: Boolean = true,
  )

  private var state = State()

  var enabled: Boolean
    get() = state.enabled
    set(value) {
      state.enabled = value
    }

  var terminalCommandsOnly: Boolean
    get() = state.terminalCommandsOnly
    set(value) {
      state.terminalCommandsOnly = value
    }

  override fun getState(): State = state

  override fun loadState(state: State) {
    XmlSerializerUtil.copyBean(state, this.state)
  }

  companion object {
    fun getInstance(): AutoAllowSettings =
      ApplicationManager.getApplication().getService(AutoAllowSettings::class.java)
  }
}
