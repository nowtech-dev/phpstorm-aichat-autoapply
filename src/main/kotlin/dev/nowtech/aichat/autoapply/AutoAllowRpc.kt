package dev.nowtech.aichat.autoapply

import com.intellij.ml.llm.chat.shared.rpc.AIAssistantChatHistoryApi
import com.intellij.ml.llm.chat.shared.rpc.TaskLauncherApi
import com.intellij.ml.llm.chat.shared.rpc.TaskLauncherApiResolver
import fleet.rpc.client.durable

internal suspend fun <T> autoAllowDurable(block: suspend () -> T): T =
  durable { block() }

internal suspend fun resolveTaskLauncher(): TaskLauncherApi =
  autoAllowDurable { TaskLauncherApiResolver.resolveTaskLauncherApi() }

internal suspend fun resolveChatHistoryApi(): AIAssistantChatHistoryApi =
  autoAllowDurable { AIAssistantChatHistoryApi.Companion.getInstance() }
