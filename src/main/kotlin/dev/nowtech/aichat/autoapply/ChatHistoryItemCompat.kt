package dev.nowtech.aichat.autoapply

import com.intellij.ml.llm.chat.shared.ChatHistorySessionItem
import com.intellij.ml.llm.chat.shared.ChatSessionId
import java.lang.reflect.Method

/** Resolves chat session id across AI Assistant API renames (2026.1 vs 2026.2+). */
internal object ChatHistoryItemCompat {
  private val chatSessionIdGetter: Method by lazy {
    val clazz = ChatHistorySessionItem::class.java
    sequenceOf("getChatSessionId", "getFrontendSessionId")
      .mapNotNull { name -> clazz.methods.firstOrNull { it.name == name && it.parameterCount == 0 } }
      .firstOrNull()
      ?: error("ChatHistorySessionItem has no ChatSessionId getter")
  }

  fun chatSessionId(item: ChatHistorySessionItem): ChatSessionId =
    chatSessionIdGetter.invoke(item) as ChatSessionId
}
