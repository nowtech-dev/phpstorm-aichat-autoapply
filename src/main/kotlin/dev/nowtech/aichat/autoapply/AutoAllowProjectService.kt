package dev.nowtech.aichat.autoapply

import com.intellij.ml.llm.aui.events.api.ApprovableBlockUpdatedEvent
import com.intellij.ml.llm.aui.events.api.ApproveInputRequest
import com.intellij.ml.llm.aui.events.api.ApproveInputResponse
import com.intellij.ml.llm.aui.events.api.ChoiceInputRequest
import com.intellij.ml.llm.aui.events.api.ChoiceInputResponse
import com.intellij.ml.llm.aui.events.api.ChoiceRequestUpdatedEvent
import com.intellij.ml.llm.aui.events.api.InputRequest
import com.intellij.ml.llm.aui.events.api.InputRequestOptionType
import com.intellij.ml.llm.aui.events.api.MessageBlockUpdatedEvent
import com.intellij.ml.llm.aui.events.api.StepStatus
import com.intellij.ml.llm.aui.events.api.StepStatusOwnerEvent
import com.intellij.ml.llm.aui.events.api.TerminalBlockUpdatedEvent
import com.intellij.ml.llm.chat.findValue
import com.intellij.ml.llm.chat.shared.ChatHistoryPage
import com.intellij.ml.llm.chat.shared.ChatHistoryQuery
import com.intellij.ml.llm.chat.shared.ChatSessionEvent
import com.intellij.ml.llm.chat.shared.ChatSessionId
import com.intellij.ml.llm.chat.shared.ChatSessionMessageBlockEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.platform.project.projectId
import dev.nowtech.aichat.autoapply.settings.AutoAllowSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class AutoAllowProjectService(private val project: Project) : Disposable {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val sessionWatchers = ConcurrentHashMap<Int, Job>()
  private val handledRequestIds =
    Collections.synchronizedSet(LinkedHashSet<String>())

  private var historyJob: Job? = null
  private var sessionPollJob: Job? = null

  fun start() {
    if (project.isDefault) {
      autoAllowLog.warn("AI Chat auto-allow: not started (default project template)")
      return
    }
    if (!AutoAllowSettings.getInstance().enabled) {
      autoAllowLog.warn("AI Chat auto-allow: not started (disabled in settings)")
      stopWatchingSessions()
      return
    }

    autoAllowLog.warn("AI Chat auto-allow: watching project \"${project.name}\"")

    historyJob?.cancel()
    sessionPollJob?.cancel()

    historyJob = scope.launch {
      runCatching { watchChatSessionsFromHistory() }
        .onFailure { e ->
          if (e is CancellationException) return@onFailure
          autoAllowLog.warn("AI Chat auto-allow: observeSessions failed", e)
        }
    }

    sessionPollJob = scope.launch {
      while (isActive) {
        runCatching { pollChatSessionsOnce() }
          .onFailure { e ->
            if (e is CancellationException) return@onFailure
            autoAllowLog.warn("AI Chat auto-allow: session poll failed", e)
          }
        delay(2_000)
      }
    }
  }

  fun restart() {
    stopWatchingSessions()
    handledRequestIds.clear()
    start()
  }

  fun stop() {
    stopWatchingSessions()
    handledRequestIds.clear()
  }

  private suspend fun watchChatSessionsFromHistory() {
    val historyApi = resolveChatHistoryApi()
    val query = ChatHistoryQuery(anchor = null, limit = 200)

    historyApi
      .observeSessions(project.projectId(), query)
      .catch { e -> autoAllowLog.warn("AI Chat auto-allow: observeSessions stream ended", e) }
      .collect { page -> registerHistoryPage(page) }
  }

  private suspend fun pollChatSessionsOnce() {
    if (!AutoAllowSettings.getInstance().enabled) return
    val historyApi = resolveChatHistoryApi()
    val query = ChatHistoryQuery(anchor = null, limit = 50)
    val page =
      withTimeoutOrNull(HISTORY_POLL_TIMEOUT_MS) {
        historyApi.observeSessions(project.projectId(), query).first()
      }
    if (page == null) {
      autoAllowLog.warn("AI Chat auto-allow: history poll timed out after ${HISTORY_POLL_TIMEOUT_MS}ms")
      return
    }
    registerHistoryPage(page)
  }

  private fun registerHistoryPage(page: ChatHistoryPage) {
    if (!AutoAllowSettings.getInstance().enabled) return
    registerSessions(page.sessions.map { ChatHistoryItemCompat.chatSessionId(it) })
    page.activeChatSessionUid?.let { activeUid ->
      page.sessions
        .firstOrNull { it.chatSessionUid == activeUid }
        ?.let { ChatHistoryItemCompat.chatSessionId(it) }
        ?.let { ensureSessionWatcher(it, preferActive = true) }
    }
  }

  private fun registerSessions(sessionIds: List<ChatSessionId>) {
    for (sessionId in sessionIds) {
      ensureSessionWatcher(sessionId)
    }
  }

  private fun ensureSessionWatcher(sessionId: ChatSessionId, preferActive: Boolean = false) {
    sessionWatchers.computeIfAbsent(sessionId.uid) { uid ->
      scope.launch {
        runCatching {
          verifySessionResolvable(sessionId, preferActive)
          collectSessionSteps(sessionId)
        }.onFailure {
          autoAllowLog.warn("AI Chat auto-allow: session watcher $uid ended", it)
        }
      }
    }
  }

  private suspend fun verifySessionResolvable(sessionId: ChatSessionId, preferActive: Boolean) {
    autoAllowDurable {
      val frontend = sessionId.findValue()
      if (frontend == null) {
        autoAllowLog.warn(
          "AI Chat auto-allow: ChatSessionId uid=${sessionId.uid} has no backend FrontendSession yet" +
            if (preferActive) " (active chat)" else "",
        )
      }
    }
  }

  private suspend fun collectSessionSteps(sessionId: ChatSessionId) {
    autoAllowDurable {
      val taskLauncherApi = resolveTaskLauncher()
      taskLauncherApi.stepsFlow(sessionId).collect { event ->
        if (!AutoAllowSettings.getInstance().enabled) return@collect
        handleSessionEvent(sessionId, event)
      }
    }
  }

  private fun handleSessionEvent(sessionId: ChatSessionId, event: ChatSessionEvent) {
    val blockEvent = event as? ChatSessionMessageBlockEvent ?: return
    handleMessageBlock(sessionId, blockEvent.event)
  }

  private fun handleMessageBlock(sessionId: ChatSessionId, updated: MessageBlockUpdatedEvent) {
    when (updated) {
      is TerminalBlockUpdatedEvent -> {
        if (!isAwaitingApproval(updated)) return
        val input = updated.approvalInputRequest ?: updated.approvalRequest ?: return
        if (!matchesTerminalFilter(updated)) return
        maybeAutoApprove(sessionId, input)
      }

      is ChoiceRequestUpdatedEvent -> {
        if (!isAwaitingApproval(updated)) return
        if (!matchesTerminalFilter(updated)) return
        maybeAutoApprove(sessionId, updated.choiceRequest)
      }

      else -> {
        if (updated !is ApprovableBlockUpdatedEvent) return
        val statusOwner = updated as? StepStatusOwnerEvent ?: return
        if (!isAwaitingApproval(statusOwner)) return
        val input = updated.approvalInputRequest ?: updated.approvalRequest ?: return
        if (AutoAllowSettings.getInstance().terminalCommandsOnly) return
        maybeAutoApprove(sessionId, input)
      }
    }
  }

  private fun isAwaitingApproval(event: StepStatusOwnerEvent): Boolean =
    when (event.status) {
      StepStatus.REJECTED,
      StepStatus.COMPLETED,
      StepStatus.CANCELED,
      StepStatus.FAILED,
      -> false
      else -> true
    }

  private fun matchesTerminalFilter(event: MessageBlockUpdatedEvent): Boolean {
    if (!AutoAllowSettings.getInstance().terminalCommandsOnly) return true
    return when (event) {
      is TerminalBlockUpdatedEvent -> true
      is ChoiceRequestUpdatedEvent -> isTerminalChoice(event)
      else -> false
    }
  }

  private fun isTerminalChoice(event: ChoiceRequestUpdatedEvent): Boolean {
    if (isAgentPermissionChoice(event)) return true

    val title = event.title.lowercase()
    if (
      title.contains("terminal") ||
        title.contains("command") ||
        title.contains("run") ||
        title.contains("shell") ||
        title.contains("execute") ||
        title.contains("allowlist") ||
        title.contains("mcp") ||
        title.contains("tool")
    ) {
      return true
    }
    return event.choiceRequest.options.any { opt ->
      val d = opt.description.lowercase()
      d.contains("allow once") ||
        d.contains("allow always") ||
        d.contains("always allow")
    }
  }

  /** Allow once / Allow always / Reject — used by integrated agents (e.g. Cursor over ACP/MCP). */
  private fun isAgentPermissionChoice(event: ChoiceRequestUpdatedEvent): Boolean {
    val kinds = event.choiceRequest.options.map { it.optionKind }.toSet()
    val hasAllowAlways = InputRequestOptionType.ALWAYS_YES in kinds
    val hasAllowOnce = InputRequestOptionType.YES in kinds
    val hasReject = InputRequestOptionType.NO in kinds
    return hasAllowAlways && (hasAllowOnce || hasReject)
  }

  private fun maybeAutoApprove(sessionId: ChatSessionId, request: InputRequest) {
    val requestKey = request.id
    if (!handledRequestIds.add(requestKey)) return
    trimHandledRequests()

    scope.launch {
      runCatching { sendAlwaysAllow(sessionId, request) }
        .onFailure {
          handledRequestIds.remove(requestKey)
          autoAllowLog.warn("AI Chat auto-allow: approval failed for $requestKey", it)
        }
    }
  }

  private suspend fun sendAlwaysAllow(sessionId: ChatSessionId, request: InputRequest) {
    autoAllowDurable {
      val frontend = sessionId.findValue()
      if (frontend == null) {
        error(
          "No FrontendSession for ChatSessionId uid=${sessionId.uid}; TaskLauncherApi.respond would be a no-op",
        )
      }

      val taskLauncherApi = resolveTaskLauncher()
      val response =
        when (request) {
          is ChoiceInputRequest -> {
            val option =
              request.options.firstOrNull { it.optionKind == InputRequestOptionType.ALWAYS_YES }
                ?: request.options.firstOrNull { it.optionKind == InputRequestOptionType.YES }
                ?: error("No Allow / Allow always option in request ${request.id}")
            ChoiceInputResponse(request.id, option.id)
          }

          is ApproveInputRequest -> ApproveInputResponse(request.id, approve = true)

          else -> return@autoAllowDurable
        }

      taskLauncherApi.respond(sessionId, response)
      autoAllowLog.warn(
        "AI Chat auto-allow: approved ${request.id} (${response::class.simpleName}, session uid=${sessionId.uid})",
      )
    }
  }

  private fun trimHandledRequests() {
    while (handledRequestIds.size > 2000) {
      val first = handledRequestIds.iterator().next()
      handledRequestIds.remove(first)
    }
  }

  private fun stopWatchingSessions() {
    historyJob?.cancel()
    historyJob = null
    sessionPollJob?.cancel()
    sessionPollJob = null
    sessionWatchers.values.forEach { it.cancel() }
    sessionWatchers.clear()
  }

  override fun dispose() {
    stopWatchingSessions()
    scope.cancel()
  }

  private companion object {
    private const val HISTORY_POLL_TIMEOUT_MS = 5_000L
  }
}
