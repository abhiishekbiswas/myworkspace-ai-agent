package com.example.ai.cloud

import android.util.Log
import com.example.ai.tools.AiToolRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Cloud AI Engine integrating with Gemini 3.8 Flash (strictly free tier, zero billing).
 * Handles function/tool calling, multi-turn history, timeouts, and quota exhaustion detection.
 */
class GeminiCloudEngine(
  private val apiKeyProvider: () -> String? = { null },
  private val httpClient: (suspend (url: String, payload: String) -> String)? = null
) : IGeminiCloudEngine {

  override val modelId: String = "gemini-3.8-flash"

  companion object {
    private const val TAG = "GeminiCloudEngine"
    private const val TIMEOUT_MS = 30_000L
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    val SYSTEM_INSTRUCTION = """
      You are the intelligent Personal AI Assistant inside the 'My Workspace' Android app.
      You help the user manage projects, tasks, checklists, dependencies, deadlines, and personal memories.
      You natively understand English, Hindi, and Hinglish.
      
      CRITICAL INSTRUCTIONS:
      1. Always use available tools for checking or modifying Workspace data. Never make up tasks or projects.
      2. If the user asks about today's tasks, pending tasks, overdue tasks, or project status, call the corresponding tool.
      3. If the user refers to past items in context (e.g. 'unmein se urgent kitne hain', 'pehle wale ko complete kar do'), use the previous conversation history and tool outputs to determine which item is referenced.
      4. If a task reference is ambiguous, ask the user to clarify instead of guessing.
      5. Keep responses direct, helpful, and concise. Format with bullet points or emojis when helpful.
    """.trimIndent()
  }

  override suspend fun generateResponse(
    prompt: String,
    history: List<AiConversationTurn>,
    tools: List<AiToolDefinition>
  ): CloudAiResult {
    return executeWithSafety {
      val payload = buildRequestPayload(prompt, history, tools, null)
      val responseText = sendRequest(payload)
      parseResponse(responseText)
    }
  }

  override suspend fun sendToolResponse(
    toolResponse: AiToolResponse,
    history: List<AiConversationTurn>,
    tools: List<AiToolDefinition>
  ): CloudAiResult {
    return executeWithSafety {
      val payload = buildRequestPayload("", history, tools, toolResponse)
      val responseText = sendRequest(payload)
      parseResponse(responseText)
    }
  }

  private suspend fun executeWithSafety(block: suspend () -> CloudAiResult): CloudAiResult {
    return try {
      withTimeout(TIMEOUT_MS) {
        block()
      }
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "Gemini request timed out after $TIMEOUT_MS ms")
      CloudAiResult.Timeout()
    } catch (e: UnknownHostException) {
      Log.w(TAG, "No internet connection: ${e.message}")
      CloudAiResult.NetworkUnavailable()
    } catch (e: ConnectException) {
      Log.w(TAG, "Connection failed: ${e.message}")
      CloudAiResult.NetworkUnavailable()
    } catch (e: SocketTimeoutException) {
      Log.w(TAG, "Socket timed out: ${e.message}")
      CloudAiResult.Timeout()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val msg = e.message ?: "Unknown error"
      if (msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || msg.contains("quota", ignoreCase = true)) {
        Log.w(TAG, "Quota limit exceeded: $msg")
        CloudAiResult.QuotaExhausted()
      } else {
        Log.e(TAG, "Gemini cloud call failed", e)
        CloudAiResult.Error(msg, e)
      }
    }
  }

  private suspend fun sendRequest(jsonPayload: String): String {
    // If a custom or mock HTTP client is provided (e.g. in unit tests), use it
    if (httpClient != null) {
      return httpClient.invoke("$BASE_URL/$modelId:generateContent", jsonPayload)
    }

    val key = apiKeyProvider()
    val urlString = if (key != null && key.isNotBlank()) {
      "$BASE_URL/$modelId:generateContent?key=$key"
    } else {
      "$BASE_URL/$modelId:generateContent"
    }

    val url = URL(urlString)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Accept", "application/json")
    conn.connectTimeout = 15_000
    conn.readTimeout = 25_000
    conn.doOutput = true

    OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
      writer.write(jsonPayload)
      writer.flush()
    }

    val responseCode = conn.responseCode
    if (responseCode == 429) {
      throw IllegalStateException("HTTP 429: RESOURCE_EXHAUSTED (Free quota limit reached)")
    }

    val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
      ?: throw IllegalStateException("HTTP $responseCode with empty response body")

    val response = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

    if (responseCode !in 200..299) {
      if (response.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || response.contains("quota", ignoreCase = true)) {
        throw IllegalStateException("HTTP $responseCode: RESOURCE_EXHAUSTED")
      }
      throw IllegalStateException("HTTP $responseCode: $response")
    }

    return response
  }

  private fun buildRequestPayload(
    prompt: String,
    history: List<AiConversationTurn>,
    tools: List<AiToolDefinition>,
    toolResponse: AiToolResponse?
  ): String {
    val root = JSONObject()

    // System instruction
    val systemObj = JSONObject()
    val sysParts = JSONArray()
    sysParts.put(JSONObject().put("text", SYSTEM_INSTRUCTION))
    systemObj.put("parts", sysParts)
    root.put("system_instruction", systemObj)

    // Contents (history + current prompt or tool response)
    val contents = JSONArray()

    for (turn in history) {
      val turnObj = JSONObject()
      val roleStr = when (turn.role) {
        AiRole.USER -> "user"
        AiRole.MODEL -> "model"
        AiRole.SYSTEM -> "system"
        AiRole.TOOL -> "function"
      }
      turnObj.put("role", roleStr)

      val parts = JSONArray()
      if (turn.toolCall != null) {
        val fc = JSONObject()
        fc.put("name", turn.toolCall.name)
        val argsObj = JSONObject()
        turn.toolCall.arguments.forEach { (k, v) -> argsObj.put(k, v) }
        fc.put("args", argsObj)
        parts.put(JSONObject().put("functionCall", fc))
      } else if (turn.toolResponse != null) {
        val fr = JSONObject()
        fr.put("name", turn.toolResponse.name)
        val respObj = JSONObject()
        turn.toolResponse.response.forEach { (k, v) -> respObj.put(k, v) }
        fr.put("response", respObj)
        parts.put(JSONObject().put("functionResponse", fr))
      } else {
        parts.put(JSONObject().put("text", turn.content))
      }
      turnObj.put("parts", parts)
      contents.put(turnObj)
    }

    // Add current user prompt if non-empty
    if (prompt.isNotBlank()) {
      val userObj = JSONObject()
      userObj.put("role", "user")
      val parts = JSONArray()
      parts.put(JSONObject().put("text", prompt))
      userObj.put("parts", parts)
      contents.put(userObj)
    }

    // Add tool response if provided
    if (toolResponse != null) {
      val toolObj = JSONObject()
      toolObj.put("role", "function")
      val parts = JSONArray()
      val fr = JSONObject()
      fr.put("name", toolResponse.name)
      val respObj = JSONObject()
      toolResponse.response.forEach { (k, v) -> respObj.put(k, v) }
      fr.put("response", respObj)
      parts.put(JSONObject().put("functionResponse", fr))
      toolObj.put("parts", parts)
      contents.put(toolObj)
    }

    root.put("contents", contents)

    // Tool declarations
    if (tools.isNotEmpty()) {
      val toolsArr = JSONArray()
      val funcDecls = JSONArray()
      for (t in tools) {
        val decl = JSONObject()
        decl.put("name", t.name)
        decl.put("description", t.description)
        if (t.parameters.isNotEmpty()) {
          val paramObj = JSONObject()
          paramObj.put("type", "OBJECT")
          val propObj = JSONObject()
          val requiredArr = JSONArray()
          for (p in t.parameters) {
            val pProp = JSONObject()
            pProp.put("type", p.type.uppercase())
            pProp.put("description", p.description)
            propObj.put(p.name, pProp)
            if (p.isRequired) {
              requiredArr.put(p.name)
            }
          }
          paramObj.put("properties", propObj)
          if (requiredArr.length() > 0) {
            paramObj.put("required", requiredArr)
          }
          decl.put("parameters", paramObj)
        }
        funcDecls.put(decl)
      }
      val toolWrapper = JSONObject()
      toolWrapper.put("function_declarations", funcDecls)
      toolsArr.put(toolWrapper)
      root.put("tools", toolsArr)
    }

    return root.toString()
  }

  fun parseResponse(jsonString: String): CloudAiResult {
    return try {
      val json = JSONObject(jsonString)
      val candidates = json.optJSONArray("candidates")
      if (candidates == null || candidates.length() == 0) {
        return CloudAiResult.Error("No candidates returned from Gemini.")
      }

      val first = candidates.getJSONObject(0)
      val content = first.optJSONObject("content")
      val parts = content?.optJSONArray("parts")

      if (parts == null || parts.length() == 0) {
        return CloudAiResult.Error("Empty content returned from Gemini.")
      }

      var textContent = ""
      var toolCall: AiToolCall? = null

      for (i in 0 until parts.length()) {
        val part = parts.getJSONObject(i)
        if (part.has("text")) {
          textContent += part.getString("text")
        }
        if (part.has("functionCall")) {
          val fc = part.getJSONObject("functionCall")
          val name = fc.getString("name")
          val argsObj = fc.optJSONObject("args")
          val argsMap = mutableMapOf<String, Any?>()
          if (argsObj != null) {
            val keys = argsObj.keys()
            while (keys.hasNext()) {
              val key = keys.next()
              argsMap[key] = argsObj.get(key)
            }
          }
          toolCall = AiToolCall(name = name, arguments = argsMap)
        }
      }

      CloudAiResult.Success(text = textContent.trim(), toolCall = toolCall)
    } catch (e: Exception) {
      CloudAiResult.Error("Failed to parse Gemini response: ${e.message}", e)
    }
  }
}
