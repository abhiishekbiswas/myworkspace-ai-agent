package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Projects : Screen("projects")
    object Tasks : Screen("tasks")
    object Kanban : Screen("kanban")
    object Calendar : Screen("calendar")
    object Analytics : Screen("analytics")
    object Notes : Screen("notes")
    object Search : Screen("search")
    object Backup : Screen("backup")
    object Settings : Screen("settings")
    object More : Screen("more")
    object Chats : Screen("chats")

    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(projectId: String) = "project_detail/$projectId"
    }

    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }

    object Conversation : Screen("conversation/{conversationId}") {
        fun createRoute(conversationId: String) = "conversation/$conversationId"
    }

    object ConversationDetail : Screen("conversation_detail/{conversationId}") {
        fun createRoute(conversationId: String) = "conversation_detail/$conversationId"
    }
}
