package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.engine.LocalCommandEngine
import com.example.ai.tools.WorkspaceToolExecutor
import com.example.data.local.database.WorkspaceDatabase
import com.example.data.repository.AiMemoryRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.WorkspaceRepository
import com.example.ui.navigation.MainNavigation
import com.example.ui.theme.MyWorkspaceTheme
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.ChatViewModelFactory
import com.example.viewmodel.WorkspaceViewModel
import com.example.viewmodel.WorkspaceViewModelFactory

class MainActivity : ComponentActivity() {

    private val workspaceViewModel: WorkspaceViewModel by viewModels {
        val database = WorkspaceDatabase.getInstance(applicationContext)
        val repository = WorkspaceRepository(database.workspaceDao())
        WorkspaceViewModelFactory(repository)
    }

    private val chatViewModel: ChatViewModel by viewModels {
        val database = WorkspaceDatabase.getInstance(applicationContext)
        val chatRepository = ChatRepository(database.chatDao())
        val workspaceRepository = WorkspaceRepository(database.workspaceDao())
        val aiMemoryRepository = AiMemoryRepository(database.aiMemoryDao())
        val toolExecutor = WorkspaceToolExecutor(workspaceRepository, aiMemoryRepository)
        val localCommandEngine = LocalCommandEngine(toolExecutor)
        ChatViewModelFactory(chatRepository, localCommandEngine)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by workspaceViewModel.themeMode.collectAsStateWithLifecycle()
            MyWorkspaceTheme(themeMode = themeMode) {
                MainNavigation(
                    viewModel = workspaceViewModel,
                    chatViewModel = chatViewModel
                )
            }
        }
    }
}
