package com.example.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.cloud.AiConversationTurn
import com.example.ai.cloud.AiExecutionSource
import com.example.ai.cloud.AiRole
import com.example.ai.cloud.AiToolCall
import com.example.ai.cloud.AiToolDefinition
import com.example.ai.cloud.AiToolResponse
import com.example.ai.cloud.CloudAiResult
import com.example.ai.cloud.IGeminiCloudEngine
import com.example.ai.coordinator.HybridAiCoordinator
import com.example.ai.domain.ToolExecutionResult
import com.example.ai.engine.LocalCommandEngine
import com.example.ai.tools.AiToolRegistry
import com.example.ai.tools.WorkspaceToolExecutor
import com.example.data.local.database.WorkspaceDatabase
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.AiMemoryRepository
import com.example.data.repository.WorkspaceRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Mock Gemini Cloud Engine that simulates responses, tool calls, timeouts, and quota exhaustion
 * without hitting the real network or consuming API quota during unit tests.
 */
class MockGeminiEngine : IGeminiCloudEngine {
  override val modelId: String = "gemini-3.8-flash"

  var mockResult: CloudAiResult = CloudAiResult.Success("Default mock response")
  var mockToolResponseResult: CloudAiResult = CloudAiResult.Success("Tool executed successfully.")

  val receivedPrompts = mutableListOf<String>()
  val receivedHistories = mutableListOf<List<AiConversationTurn>>()
  val receivedToolResponses = mutableListOf<AiToolResponse>()

  override suspend fun generateResponse(
    prompt: String,
    history: List<AiConversationTurn>,
    tools: List<AiToolDefinition>
  ): CloudAiResult {
    receivedPrompts.add(prompt)
    receivedHistories.add(history.toList())
    return mockResult
  }

  override suspend fun sendToolResponse(
    toolResponse: AiToolResponse,
    history: List<AiConversationTurn>,
    tools: List<AiToolDefinition>
  ): CloudAiResult {
    receivedToolResponses.add(toolResponse)
    receivedHistories.add(history.toList())
    return mockToolResponseResult
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HybridAiCoordinatorTest {

  private lateinit var database: WorkspaceDatabase
  private lateinit var workspaceRepository: WorkspaceRepository
  private lateinit var aiMemoryRepository: AiMemoryRepository
  private lateinit var toolExecutor: WorkspaceToolExecutor
  private lateinit var toolRegistry: AiToolRegistry
  private lateinit var localEngine: LocalCommandEngine
  private lateinit var mockCloudEngine: MockGeminiEngine
  private lateinit var coordinator: HybridAiCoordinator

  private lateinit var defaultProject: ProjectEntity

  @Before
  fun setUp() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = WorkspaceDatabase.createInMemory(context)
    workspaceRepository = WorkspaceRepository(database.workspaceDao())
    aiMemoryRepository = AiMemoryRepository(database.aiMemoryDao())
    toolExecutor = WorkspaceToolExecutor(workspaceRepository, aiMemoryRepository)
    toolRegistry = AiToolRegistry(toolExecutor)
    localEngine = LocalCommandEngine(toolExecutor)
    mockCloudEngine = MockGeminiEngine()
    coordinator = HybridAiCoordinator(mockCloudEngine, localEngine, toolRegistry)

    defaultProject = ProjectEntity(
      id = "proj-1",
      name = "Core App",
      status = "Active"
    )
    workspaceRepository.saveProject(defaultProject)
  }

  @After
  fun tearDown() {
    database.close()
  }

  // 1. Cloud AI successful response
  @Test
  fun testCloudAiSuccessfulResponse() = runBlocking {
    mockCloudEngine.mockResult = CloudAiResult.Success("Hello from Gemini 3.8 Flash!")

    val response = coordinator.processUserMessage("Hi there")
    assertEquals(AiExecutionSource.CLOUD_GEMINI, response.source)
    assertEquals("Hello from Gemini 3.8 Flash!", response.text)
    assertFalse(response.quotaNotice)
  }

  // 2. Tool call parsing and 3. Tool execution and result return
  @Test
  fun testToolCallExecutionAndResultReturn() = runBlocking {
    // Simulate model deciding to call getPendingTasks
    val toolCall = AiToolCall(
      name = "getPendingTasks",
      arguments = emptyMap()
    )
    mockCloudEngine.mockResult = CloudAiResult.Success(
      text = "Let me check your pending tasks.",
      toolCall = toolCall
    )
    mockCloudEngine.mockToolResponseResult = CloudAiResult.Success(
      text = "You have 1 pending task in Core App."
    )

    // Pre-create a task
    val task = TaskEntity(id = "t1", projectId = defaultProject.id, title = "Write Tests", status = "To Do")
    workspaceRepository.saveTask(task)

    val response = coordinator.processUserMessage("What tasks are left?")
    assertEquals(AiExecutionSource.CLOUD_GEMINI, response.source)
    assertEquals("You have 1 pending task in Core App.", response.text)

    // Verify tool response was delivered to model
    assertEquals(1, mockCloudEngine.receivedToolResponses.size)
    assertEquals("getPendingTasks", mockCloudEngine.receivedToolResponses.first().name)
    val respMap = mockCloudEngine.receivedToolResponses.first().response
    assertEquals("success", respMap["status"])
  }

  // 4. Multi-turn conversation context
  @Test
  fun testMultiTurnConversationContextMaintained() = runBlocking {
    mockCloudEngine.mockResult = CloudAiResult.Success("I can help with that.")
    coordinator.processUserMessage("I am working on Core App.")

    mockCloudEngine.mockResult = CloudAiResult.Success("Core App has 3 pending tasks.")
    coordinator.processUserMessage("What is its status?")

    val history = coordinator.conversationHistory
    assertTrue("Should retain turns in history", history.size >= 4)
    assertEquals(AiRole.USER, history[0].role)
    assertEquals("I am working on Core App.", history[0].content)
    assertEquals(AiRole.MODEL, history[1].role)
    assertEquals("I can help with that.", history[1].content)
    assertEquals(AiRole.USER, history[2].role)
    assertEquals("What is its status?", history[2].content)
  }

  // 5. Offline routing when network is unavailable
  @Test
  fun testOfflineRoutingWhenNoNetwork() = runBlocking {
    coordinator.setNetworkAvailable(false)

    // Should route directly to LocalCommandEngine without hitting cloud
    val response = coordinator.processUserMessage("Show all pending tasks")
    assertEquals(AiExecutionSource.OFFLINE_FALLBACK_OFFLINE_MODE, response.source)
    assertTrue(response.text.contains("Offline Mode Active"))
    assertEquals(0, mockCloudEngine.receivedPrompts.size)
  }

  // 6. Cloud AI disabled by user setting
  @Test
  fun testCloudAiDisabledByUserSetting() = runBlocking {
    coordinator.setCloudAiEnabled(false)

    val response = coordinator.processUserMessage("Pending tasks dikhao")
    assertEquals(AiExecutionSource.OFFLINE_USER_DISABLED, response.source)
    assertEquals(0, mockCloudEngine.receivedPrompts.size)
  }

  // 7. HTTP 429 quota exhaustion fallback
  @Test
  fun testHttp429QuotaExhaustionFallback() = runBlocking {
    mockCloudEngine.mockResult = CloudAiResult.QuotaExhausted()

    val response = coordinator.processUserMessage("Show all pending tasks")
    assertEquals(AiExecutionSource.OFFLINE_FALLBACK_QUOTA_EXHAUSTED, response.source)
    assertTrue("Should contain quota warning", response.quotaNotice)
    assertTrue(response.text.contains("Free Quota Limit"))
    assertTrue(coordinator.isQuotaExhausted.value)

    // Next request should immediately route offline because quota is marked exhausted
    val nextResponse = coordinator.processUserMessage("Show all pending tasks")
    assertEquals(AiExecutionSource.OFFLINE_FALLBACK_QUOTA_EXHAUSTED, nextResponse.source)
  }

  // 8. Network timeout fallback
  @Test
  fun testNetworkTimeoutFallback() = runBlocking {
    mockCloudEngine.mockResult = CloudAiResult.Timeout()

    val response = coordinator.processUserMessage("Pending tasks")
    assertEquals(AiExecutionSource.OFFLINE_FALLBACK_NETWORK_TIMEOUT, response.source)
    assertTrue(response.text.contains("Cloud Request Timed Out"))
  }

  // 9. Provider failure fallback
  @Test
  fun testProviderFailureFallback() = runBlocking {
    mockCloudEngine.mockResult = CloudAiResult.Error("Internal server error 500")

    val response = coordinator.processUserMessage("Show all pending tasks")
    assertEquals(AiExecutionSource.OFFLINE_COMMAND_ENGINE, response.source)
  }

  // 10. Duplicate action prevention
  @Test
  fun testDuplicateActionPrevention() = runBlocking {
    val createCall = AiToolCall(
      name = "createTask",
      arguments = mapOf("title" to "Submit Build Report", "projectName" to "Core App")
    )
    mockCloudEngine.mockResult = CloudAiResult.Success(text = "Creating task...", toolCall = createCall)
    mockCloudEngine.mockToolResponseResult = CloudAiResult.Success("Task created.")

    val first = coordinator.processUserMessage("Create task Submit Build Report")
    assertEquals(AiExecutionSource.CLOUD_GEMINI, first.source)

    // Second identical action should be caught by idempotency cache
    val second = coordinator.processUserMessage("Create task Submit Build Report")
    assertTrue(second.text.contains("preventing duplicate", ignoreCase = true))
  }

  // 11. Tool parameter validation
  @Test
  fun testToolParameterValidation() = runBlocking {
    // Missing required title
    val invalidCall = AiToolCall(name = "createTask", arguments = emptyMap())
    val result = toolRegistry.executeTool(invalidCall)
    assertTrue("Missing title should return Error", result is ToolExecutionResult.Error)
    assertTrue((result as ToolExecutionResult.Error).message.contains("Missing required parameter: title"))
  }

  // 12. Ambiguous task references and clarification
  @Test
  fun testAmbiguousTaskReferencesRequestClarification() = runBlocking {
    val p2 = ProjectEntity(id = "p2", name = "Web App", status = "Active")
    workspaceRepository.saveProject(p2)

    val t1 = TaskEntity(id = "ta", projectId = defaultProject.id, title = "Design Logo")
    val t2 = TaskEntity(id = "tb", projectId = p2.id, title = "Design Logo")
    workspaceRepository.saveTask(t1)
    workspaceRepository.saveTask(t2)

    val completeCall = AiToolCall(name = "completeTask", arguments = mapOf("taskTitleOrId" to "Design Logo"))
    mockCloudEngine.mockResult = CloudAiResult.Success(text = "Completing...", toolCall = completeCall)

    val response = coordinator.processUserMessage("Mark Design Logo done")
    assertTrue("Should require clarification for ambiguous tasks", response.toolResult is ToolExecutionResult.ClarificationRequired)
  }
}
