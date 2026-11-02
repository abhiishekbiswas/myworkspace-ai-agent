package com.example.ai.cloud

interface IGeminiCloudEngine {
  /**
   * The officially supported zero-cost model identifier.
   */
  val modelId: String

  /**
   * Sends prompt with multi-turn history and available tools to the Gemini API.
   */
  suspend fun generateResponse(
    prompt: String,
    history: List<AiConversationTurn> = emptyList(),
    tools: List<AiToolDefinition> = emptyList()
  ): CloudAiResult

  /**
   * Sends tool execution results back to the model to finalize the multi-turn answer.
   */
  suspend fun sendToolResponse(
    toolResponse: AiToolResponse,
    history: List<AiConversationTurn>,
    tools: List<AiToolDefinition> = emptyList()
  ): CloudAiResult
}
