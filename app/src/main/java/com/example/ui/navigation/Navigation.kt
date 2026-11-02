package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.screens.chat.ChatsScreen
import com.example.ui.screens.chat.ConversationDetailScreen
import com.example.ui.screens.chat.ConversationScreen
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.WorkspaceViewModel

@Composable
fun MainNavigation(
    viewModel: WorkspaceViewModel,
    chatViewModel: ChatViewModel,
    navController: NavHostController = rememberNavController()
) {
    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Screen.Dashboard.route, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        BottomNavItem("Projects", Screen.Projects.route, Icons.Filled.Folder, Icons.Outlined.Folder),
        BottomNavItem("Tasks", Screen.Tasks.route, Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
        BottomNavItem("Chats", Screen.Chats.route, Icons.Filled.Chat, Icons.Outlined.Chat),
        BottomNavItem("More", Screen.More.route, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentDestination }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentDestination != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag("nav_${item.title.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToTaskDetail = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                    onNavigateToProjectDetail = { projectId -> navController.navigate(Screen.ProjectDetail.createRoute(projectId)) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToChats = { navController.navigate(Screen.Chats.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Projects.route) {
                ProjectsScreen(
                    viewModel = viewModel,
                    onProjectClick = { projectId -> navController.navigate(Screen.ProjectDetail.createRoute(projectId)) }
                )
            }

            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                ProjectDetailScreen(
                    projectId = projectId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) }
                )
            }

            composable(Screen.Tasks.route) {
                TasksScreen(
                    viewModel = viewModel,
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) }
                )
            }

            composable(
                route = Screen.TaskDetail.route,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                TaskDetailScreen(
                    taskId = taskId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Kanban.route) {
                KanbanScreen(
                    viewModel = viewModel,
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) }
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(
                    viewModel = viewModel,
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) }
                )
            }

            composable(Screen.Notes.route) {
                NotesScreen(viewModel = viewModel)
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                    onProjectClick = { projectId -> navController.navigate(Screen.ProjectDetail.createRoute(projectId)) }
                )
            }

            composable(Screen.Backup.route) {
                BackupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    viewModel = viewModel,
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                    onNavigateToKanban = { navController.navigate(Screen.Kanban.route) },
                    onNavigateToNotes = { navController.navigate(Screen.Notes.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Chats.route) {
                ChatsScreen(
                    chatViewModel = chatViewModel,
                    onConversationClick = { convId -> navController.navigate(Screen.Conversation.createRoute(convId)) }
                )
            }

            composable(
                route = Screen.Conversation.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                ConversationScreen(
                    conversationId = conversationId,
                    chatViewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onConversationDetailClick = { navController.navigate(Screen.ConversationDetail.createRoute(conversationId)) }
                )
            }

            composable(
                route = Screen.ConversationDetail.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                ConversationDetailScreen(
                    conversationId = conversationId,
                    chatViewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
