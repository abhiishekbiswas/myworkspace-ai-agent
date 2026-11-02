package com.example.ai.cloud

/**
 * Roles for conversation turns in AI interactions.
 */
enum class AiRole {
  USER,
  MODEL,
  SYSTEM,
  TOOL
}

/**
 * Representation of a tool call requested by the model.
 */
data class AiToolCall(
  val name: String,
  val arguments: Map<String, Any?> = emptyMap(),
  val callId: String = java.util.UUID.randomUUID().toString()
)

/**
 * Representation of a tool result returned back to the model.
 */
data class AiToolResponse(
  val name: String,
  val response: Map<String, Any?>,
  val callId: String? = null
)

/**
 * A single turn in a multi-turn conversation.
 */
data class AiConversationTurn(
  val role: AiRole,
  val content: String,
  val toolCall: AiToolCall? = null,
  val toolResponse: AiToolResponse? = null,
  val timestamp: Long = System.currentTimeMillis()
)

/**
 * Parameter definition for a tool/function exposed to the model.
 */
data class AiToolParameter(
  val name: String,
  val type: String, // "string", "number", "boolean", "integer"
  val description: String,
  val isRequired: Boolean = true
)

/**
 * Tool/Function definition exposed to Gemini.
 */
data class AiToolDefinition(
  val name: String,
  val description: String,
  val parameters: List<AiToolParameter> = emptyList()
)

/**
 * Source of the AI execution.
 */
enum class AiExecutionSource {
  CLOUD_GEMINI,
  OFFLINE_COMMAND_ENGINE,
  OFFLINE_FALLBACK_QUOTA_EXHAUSTED,
  OFFLINE_FALLBACK_NETWORK_TIMEOUT,
  OFFLINE_FALLBACK_OFFLINE_MODE,
  OFFLINE_USER_DISABLED
}

/**
 * Result of calling a cloud AI model.
 */
sealed class CloudAiResult {
  data class Success(
    val text: String,
    val toolCall: AiToolCall? = null
  ) : CloudAiResult()

  data class QuotaExhausted(
    val message: String = "Free tier quota exhausted (HTTP 429). Switching to offline command engine."
  ) : CloudAiResult()

  data class Timeout(
    val message: String = "Cloud AI request timed out. Switching to offline command engine."
  ) : CloudAiResult()

  data class NetworkUnavailable(
    val message: String = "Network is unavailable. Switching to offline command engine."
  ) : CloudAiResult()

  data class Error(
    val message: String,
    val cause: Throwable? = null
  ) : CloudAiResult()
}
