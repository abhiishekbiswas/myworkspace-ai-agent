package com.example.ai.coordinator

import android.util.Log
import com.example.ai.cloud.AiConversationTurn
import com.example.ai.cloud.AiExecutionSource
import com.example.ai.cloud.AiRole
import com.example.ai.cloud.AiToolResponse
import com.example.ai.cloud.CloudAiResult
import com.example.ai.cloud.IGeminiCloudEngine
import com.example.ai.domain.ToolExecutionResult
import com.example.ai.engine.LocalCommandEngine
import com.example.ai.tools.AiToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

/**
 * Result returned by the Hybrid AI Coordinator to the UI layer.
 */
data class HybridAiResponse(
  val text: String,
  val source: AiExecutionSource,
  val toolResult: ToolExecutionResult? = null,
  val quotaNotice: Boolean = false
)

/**
 * Hybrid AI Coordinator orchestrating intelligent online Gemini 3.8 Flash reasoning
 * and offline LocalCommandEngine execution with zero-cost safeguards and automatic fallback.
 */
class HybridAiCoordinator(
  private val cloudEngine: IGeminiCloudEngine,
  private val localEngine: LocalCommandEngine,
  private val toolRegistry: AiToolRegistry
) {

  companion object {
    private const val TAG = "HybridAiCoordinator"
  }

  // --- USER CONTROLS & STATUS FLAGS ---

  private val _isCloudAiEnabled = MutableStateFlow(true)
  val isCloudAiEnabled: StateFlow<Boolean> = _isCloudAiEnabled.asStateFlow()

  private val _isNetworkAvailable = MutableStateFlow(true)
  val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

  private val _isQuotaExhausted = MutableStateFlow(false)
  val isQuotaExhausted: StateFlow<Boolean> = _isQuotaExhausted.asStateFlow()

  private val _conversationHistory = mutableListOf<AiConversationTurn>()
  val conversationHistory: List<AiConversationTurn> get() = _conversationHistory.toList()

  // Idempotency cache: prevents duplicate execution of identical write tool calls during retries
  private val executedActionSignatures = Collections.synchronizedSet(mutableSetOf<String>())

  fun setCloudAiEnabled(enabled: Boolean) {
    _isCloudAiEnabled.value = enabled
  }

  fun setNetworkAvailable(available: Boolean) {
    _isNetworkAvailable.value = available
  }

  fun resetQuotaStatus() {
    _isQuotaExhausted.value = false
  }

  fun resetConversation() {
    _conversationHistory.clear()
    executedActionSignatures.clear()
  }

  /**
   * Main entrypoint for processing user messages.
   * Routes between Gemini 3.8 Flash and LocalCommandEngine with strict cost protection.
   */
  suspend fun processUserMessage(prompt: String): HybridAiResponse {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isEmpty()) {
      return HybridAiResponse(
        text = "Please enter a message or command.",
        source = AiExecutionSource.OFFLINE_COMMAND_ENGINE
      )
    }

    // 1. ROUTING CHECK: User disabled Cloud AI
    if (!_isCloudAiEnabled.value) {
      Log.d(TAG, "Routing to LocalCommandEngine: Cloud AI disabled by user setting")
      return executeLocally(cleanPrompt, AiExecutionSource.OFFLINE_USER_DISABLED)
    }

    // 2. ROUTING CHECK: Offline / No network
    if (!_isNetworkAvailable.value) {
      Log.d(TAG, "Routing to LocalCommandEngine: Network unavailable")
      val localRes = executeLocally(cleanPrompt, AiExecutionSource.OFFLINE_FALLBACK_OFFLINE_MODE)
      return localRes.copy(
        text = "📡 [Offline Mode Active]\n${localRes.text}"
      )
    }

    // 3. ROUTING CHECK: Free quota exhausted (HTTP 429)
    if (_isQuotaExhausted.value) {
      Log.d(TAG, "Routing to LocalCommandEngine: Free quota exhausted")
      val localRes = executeLocally(cleanPrompt, AiExecutionSource.OFFLINE_FALLBACK_QUOTA_EXHAUSTED)
      return localRes.copy(
        text = "⚠️ [Free Quota Limit Active]\n${localRes.text}",
        quotaNotice = true
      )
    }

    // 4. ATTEMPT CLOUD AI VIA GEMINI 3.8 FLASH
    return try {
      val result = cloudEngine.generateResponse(
        prompt = cleanPrompt,
        history = _conversationHistory,
        tools = toolRegistry.availableTools
      )

      handleCloudResult(cleanPrompt, result)
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected exception in Cloud AI, falling back to local engine", e)
      executeLocally(cleanPrompt, AiExecutionSource.OFFLINE_COMMAND_ENGINE)
    }
  }

  private suspend fun handleCloudResult(
    userPrompt: String,
    cloudResult: CloudAiResult
  ): HybridAiResponse {
    return when (cloudResult) {
      is CloudAiResult.Success -> {
        // Record user turn in history
        _conversationHistory.add(AiConversationTurn(role = AiRole.USER, content = userPrompt))

        val toolCall = cloudResult.toolCall
        if (toolCall != null) {
          // Record model tool call in history
          _conversationHistory.add(
            AiConversationTurn(
              role = AiRole.MODEL,
              content = cloudResult.text,
              toolCall = toolCall
            )
          )

          // Check Idempotency for write operations (createTask, updateTask, completeTask, saveMemory)
          val actionSignature = "${toolCall.name}:${toolCall.arguments}"
          val isWriteAction = toolCall.name in setOf("createTask", "updateTask", "completeTask", "createReminder", "saveMemory")
          if (isWriteAction && executedActionSignatures.contains(actionSignature)) {
            Log.w(TAG, "Duplicate write action prevented: $actionSignature")
            return HybridAiResponse(
              text = "This action has already been performed. Preventing duplicate execution.",
              source = AiExecutionSource.CLOUD_GEMINI
            )
          }

          // Execute tool locally against Workspace repositories
          val toolExecResult = toolRegistry.executeTool(toolCall)
          if (isWriteAction && toolExecResult is ToolExecutionResult.Success) {
            executedActionSignatures.add(actionSignature)
          }

          // If tool requires user clarification or confirmation, surface immediately to user
          if (toolExecResult is ToolExecutionResult.ClarificationRequired ||
            toolExecResult is ToolExecutionResult.ConfirmationRequired
          ) {
            return HybridAiResponse(
              text = when (toolExecResult) {
                is ToolExecutionResult.ClarificationRequired -> toolExecResult.prompt
                is ToolExecutionResult.ConfirmationRequired -> "${toolExecResult.prompt}\n⚠️ ${toolExecResult.warning}"
                else -> ""
              },
              source = AiExecutionSource.CLOUD_GEMINI,
              toolResult = toolExecResult
            )
          }

          // Send tool response back to Gemini to produce final conversational response
          val toolResponseMap = toolRegistry.formatToolResultForModel(toolExecResult)
          val toolResponse = AiToolResponse(
            name = toolCall.name,
            response = toolResponseMap,
            callId = toolCall.callId
          )

          _conversationHistory.add(
            AiConversationTurn(
              role = AiRole.TOOL,
              content = "",
              toolResponse = toolResponse
            )
          )

          val finalResponse = cloudEngine.sendToolResponse(
            toolResponse = toolResponse,
            history = _conversationHistory,
            tools = toolRegistry.availableTools
          )

          when (finalResponse) {
            is CloudAiResult.Success -> {
              _conversationHistory.add(
                AiConversationTurn(role = AiRole.MODEL, content = finalResponse.text)
              )
              HybridAiResponse(
                text = finalResponse.text,
                source = AiExecutionSource.CLOUD_GEMINI,
                toolResult = toolExecResult
              )
            }
            else -> {
              // Fallback to local human message if follow-up cloud request fails
              val fallbackText = when (toolExecResult) {
                is ToolExecutionResult.Success -> toolExecResult.message
                is ToolExecutionResult.Error -> toolExecResult.message
                else -> "Action completed."
              }
              HybridAiResponse(
                text = fallbackText,
                source = AiExecutionSource.CLOUD_GEMINI,
                toolResult = toolExecResult
              )
            }
          }
        } else {
          // Direct text answer without tools
          _conversationHistory.add(
            AiConversationTurn(role = AiRole.MODEL, content = cloudResult.text)
          )
          HybridAiResponse(
            text = cloudResult.text,
            source = AiExecutionSource.CLOUD_GEMINI
          )
        }
      }

      is CloudAiResult.QuotaExhausted -> {
        _isQuotaExhausted.value = true
        Log.w(TAG, "Cloud AI Quota exhausted (HTTP 429). Falling back to offline LocalCommandEngine.")
        val localRes = executeLocally(userPrompt, AiExecutionSource.OFFLINE_FALLBACK_QUOTA_EXHAUSTED)
        localRes.copy(
          text = "⚠️ [Free Quota Limit Reached — Switched to Offline Engine]\n${localRes.text}",
          quotaNotice = true
        )
      }

      is CloudAiResult.Timeout -> {
        Log.w(TAG, "Cloud AI timed out. Falling back to offline LocalCommandEngine.")
        val localRes = executeLocally(userPrompt, AiExecutionSource.OFFLINE_FALLBACK_NETWORK_TIMEOUT)
        localRes.copy(
          text = "⏱️ [Cloud Request Timed Out — Handled Offline]\n${localRes.text}"
        )
      }

      is CloudAiResult.NetworkUnavailable -> {
        _isNetworkAvailable.value = false
        Log.w(TAG, "Network unavailable. Falling back to offline LocalCommandEngine.")
        val localRes = executeLocally(userPrompt, AiExecutionSource.OFFLINE_FALLBACK_OFFLINE_MODE)
        localRes.copy(
          text = "📡 [Offline Mode Active]\n${localRes.text}"
        )
      }

      is CloudAiResult.Error -> {
        Log.e(TAG, "Cloud AI returned error: ${cloudResult.message}. Falling back to offline engine.")
        executeLocally(userPrompt, AiExecutionSource.OFFLINE_COMMAND_ENGINE)
      }
    }
  }

  private suspend fun executeLocally(
    prompt: String,
    source: AiExecutionSource
  ): HybridAiResponse {
    val cmdResult = localEngine.execute(prompt)
    val text = when (val res = cmdResult.executionResult) {
      is ToolExecutionResult.Success -> res.message
      is ToolExecutionResult.Error -> res.message
      is ToolExecutionResult.ClarificationRequired -> res.prompt
      is ToolExecutionResult.ConfirmationRequired -> "${res.prompt}\n⚠️ ${res.warning}"
    }

    return HybridAiResponse(
      text = text,
      source = source,
      toolResult = cmdResult.executionResult
    )
  }
}
