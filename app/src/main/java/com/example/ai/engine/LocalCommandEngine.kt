package com.example.ai.engine

import com.example.ai.domain.ToolExecutionResult
import com.example.ai.tools.WorkspaceToolExecutor
import java.util.Locale

class LocalCommandEngine(
    private val executor: WorkspaceToolExecutor
) {
    fun parse(input: String): CommandIntent {
        val clean = input.trim()
        if (clean.isEmpty()) return CommandIntent.Unknown(input)

        val lower = clean.lowercase(Locale.getDefault())

        if (isPendingTasksQuery(lower)) return CommandIntent.GetPendingTasks
        if (isTodayTasksQuery(lower)) return CommandIntent.GetTodayTasks
        if (isTomorrowTasksQuery(lower)) return CommandIntent.GetTomorrowTasks
        if (isOverdueTasksQuery(lower)) return CommandIntent.GetOverdueTasks

        val searchMemoryIntent = matchSearchMemory(lower)
        if (searchMemoryIntent != null) return searchMemoryIntent

        val saveMemoryIntent = matchSaveMemory(clean, lower)
        if (saveMemoryIntent != null) return saveMemoryIntent

        val completeIntent = matchCompleteTask(clean, lower)
        if (completeIntent != null) return completeIntent

        val createIntent = matchCreateTask(clean, lower)
        if (createIntent != null) return createIntent

        val projectStatusIntent = matchProjectStatus(clean, lower)
        if (projectStatusIntent != null) return projectStatusIntent

        return CommandIntent.Unknown(input)
    }

    suspend fun execute(input: String): CommandResult {
        val intent = parse(input)
        return when (intent) {
            is CommandIntent.GetTodayTasks -> {
                val res = executor.getTodayTasks()
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.GetTomorrowTasks -> {
                val res = executor.getTomorrowTasks()
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.GetPendingTasks -> {
                val res = executor.getPendingTasks()
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.GetOverdueTasks -> {
                val res = executor.getOverdueTasks()
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.GetProjectStatus -> {
                val res = executor.getProjectStatus(intent.projectName)
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.CreateTask -> {
                val res = executor.createTask(
                    title = intent.title,
                    projectName = intent.projectName,
                    priority = intent.priority,
                    dueDate = intent.dueDate
                )
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.CompleteTask -> {
                val res = executor.completeTask(
                    taskTitleOrId = intent.taskTitleOrId,
                    forceIfBlocked = intent.forceIfBlocked
                )
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.UpdateTask -> {
                val res = executor.updateTask(
                    taskTitleOrId = intent.taskTitleOrId,
                    newStatus = intent.newStatus,
                    newPriority = intent.newPriority,
                    progress = intent.progress
                )
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.CreateReminder -> {
                val res = executor.createReminder(
                    taskTitleOrId = intent.taskTitleOrId,
                    dueDateMs = intent.dueDateMs
                )
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.SaveMemory -> {
                val res = executor.saveMemory(
                    category = intent.category,
                    fact = intent.fact
                )
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.SearchMemory -> {
                val res = executor.searchMemory(intent.query)
                CommandResult(formatResult(res), intent, res)
            }
            is CommandIntent.Unknown -> {
                val help = "🤖 I didn't recognize that command. You can ask me:\n" +
                        "• 'आज कितने काम बाकी हैं?' or 'Show my pending tasks'\n" +
                        "• 'Create a task called Electricity Bill'\n" +
                        "• 'Electricity bill ko complete mark karo'\n" +
                        "• 'Project Alpha ka status kya hai?'\n" +
                        "• 'Yaad rakhna mujhe weekly reports pasand hain'\n" +
                        "• 'Maine kya yaad rakhne ko bola tha?'"
                CommandResult(help, intent, null)
            }
        }
    }

    private fun isPendingTasksQuery(lower: String): Boolean {
        val norm = lower.replace("?", "").replace("!", "").replace(".", "").trim()
        if (norm.contains("आज कितने") || norm.contains("aaj kitne")) return false
        val hasPendingWord = norm.contains("pending") || norm.contains("पेंडिंग") || norm.contains("बाकी") || norm.contains("अधूरे")
        val hasTaskWord = norm.contains("task") || norm.contains("tasks") || norm.contains("टास्क") || norm.contains("काम") ||
                norm.contains("batao") || norm.contains("show") || norm.contains("दिखाओ") || norm.contains("बताओ")
        return (hasPendingWord && hasTaskWord) ||
                norm == "pending tasks" || norm == "show pending tasks" ||
                norm.contains("pending tasks") || norm.contains("पेंडिंग टास्क")
    }

    private fun isTodayTasksQuery(lower: String): Boolean {
        val norm = lower.replace("?", "").replace("!", "").replace(".", "").trim()
        val hindiToday = listOf("आज", "aaj", "today")
        val taskWords = listOf("काम", "टास्क", "task", "tasks", "schedule", "agenda", "plan", "बाकी", "baaki", "baki")
        val hasToday = hindiToday.any { norm.contains(it) }
        val hasTask = taskWords.any { norm.contains(it) } || norm.contains("kya hai") || norm.contains("कितने")
        return hasToday && hasTask
    }

    private fun isTomorrowTasksQuery(lower: String): Boolean {
        val norm = lower.replace("?", "").replace("!", "").replace(".", "").trim()
        val hindiTomorrow = listOf("कल", "kal", "tomorrow")
        val taskWords = listOf("काम", "टास्क", "task", "tasks", "schedule", "agenda", "due", "karna", "करना", "kya", "क्या")
        return hindiTomorrow.any { norm.contains(it) } && taskWords.any { norm.contains(it) }
    }

    private fun isOverdueTasksQuery(lower: String): Boolean {
        val norm = lower.replace("?", "").replace("!", "").replace(".", "").trim()
        return norm.contains("overdue") || norm.contains("late task") || norm.contains("late tasks") ||
                norm.contains("missed deadline") || norm.contains("तारीख निकल गई") || norm.contains("देर से")
    }

    private fun matchSearchMemory(lower: String): CommandIntent? {
        val norm = lower.replace("?", "").replace("!", "").replace(".", "").trim()
        val phrases = listOf(
            "maine kya yaad rakhne ko bola tha",
            "maine kya bola tha yaad rakhne ko",
            "what did i ask you to remember",
            "what did i tell you to remember",
            "show memories",
            "what do you remember",
            "kya yaad hai",
            "मैंने क्या याद रखने को कहा था",
            "याददाश्त दिखाओ",
            "list memories"
        )
        if (phrases.any { norm.contains(it) }) {
            return CommandIntent.SearchMemory("")
        }
        val memoryRegex = Regex("(?:search|find|batao|kya hai).*?(?:memory|yaad|fact)\\s+(?:about|for|regarding)?\\s*(.+)", RegexOption.IGNORE_CASE)
        val match = memoryRegex.find(cleanPunctuation(lower))
        if (match != null) {
            val q = match.groupValues[1].trim()
            if (q.isNotEmpty()) return CommandIntent.SearchMemory(q)
        }
        return null
    }

    private fun matchSaveMemory(clean: String, lower: String): CommandIntent? {
        val yaadRakhnaRegex = Regex("(?:yaad rakhna|remember that|remember|याद रखना)\\s*(?:ki|that|:)?\\s*(.+)", RegexOption.IGNORE_CASE)
        val match = yaadRakhnaRegex.find(clean)
        if (match != null) {
            val fact = match.groupValues[1].trim()
            if (fact.isNotEmpty()) {
                val category = if (fact.contains("pasand", ignoreCase = true) || fact.contains("like", ignoreCase = true) || fact.contains("prefer", ignoreCase = true)) {
                    "Preference"
                } else {
                    "General"
                }
                return CommandIntent.SaveMemory(category, fact)
            }
        }
        return null
    }

    private fun matchCompleteTask(clean: String, lower: String): CommandIntent? {
        val norm = cleanPunctuation(clean)

        // 1. English with 'as complete/done/finished': "Mark [the] [task] [Task] as done / complete / completed / finished"
        val markAsPattern = Regex("^(?:mark\\s+)?(?:the\\s+)?(?:task\\s+)?(.+?)\\s+as\\s+(?:complete|completed|done|finished)$", RegexOption.IGNORE_CASE)
        val markAsMatch = markAsPattern.find(norm)
        if (markAsMatch != null) {
            val title = markAsMatch.groupValues[1].trim()
            if (title.isNotEmpty()) return CommandIntent.CompleteTask(title)
        }

        // 2. English verb-first: "Complete / Finish / Done [the] [task] [Task]"
        val completeVerbPattern = Regex("^(?:complete|finish|mark complete|done)\\s+(?:the\\s+)?(?:task\\s+)?(.+)$", RegexOption.IGNORE_CASE)
        val completeVerbMatch = completeVerbPattern.find(norm)
        if (completeVerbMatch != null) {
            val title = completeVerbMatch.groupValues[1].trim()
            if (title.isNotEmpty()) return CommandIntent.CompleteTask(title)
        }

        // 3. Hindi: "[Task] काम पूरा हो गया"
        val hindiPattern = Regex("^(.+?)\\s+(?:का\\s+)?(?:काम\\s+)?पूरा\\s+हो\\s+गया$", RegexOption.IGNORE_CASE)
        val hindiMatch = hindiPattern.find(norm)
        if (hindiMatch != null) {
            val title = hindiMatch.groupValues[1].trim()
            if (title.isNotEmpty()) return CommandIntent.CompleteTask(title)
        }

        // 4. Hinglish: "[Task] ko complete mark karo" or "[Task] complete karo / kar do / khatam karo"
        val hinglishPatterns = listOf(
            Regex("^(.+?)\\s+ko\\s+(?:complete|finish|khatam|done)(?:\\s+mark)?(?:\\s+(?:karo|kar do|kar dena|kijiye))?$", RegexOption.IGNORE_CASE),
            Regex("^(.+?)\\s+(?:complete|finish|khatam|done)(?:\\s+mark)?\\s+(?:karo|kar do|kar dena|kijiye)$", RegexOption.IGNORE_CASE)
        )
        for (pattern in hinglishPatterns) {
            val match = pattern.find(norm)
            if (match != null) {
                val title = match.groupValues[1].trim()
                    .replace(Regex("^(the|a|task|mark)\\s+", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (title.isNotEmpty() && !title.equals("task", ignoreCase = true)) {
                    return CommandIntent.CompleteTask(title)
                }
            }
        }

        return null
    }


    private fun matchCreateTask(clean: String, lower: String): CommandIntent? {
        val patterns = listOf(
            Regex("(?:create|add|make)\\s+(?:a\\s+)?(?:task|todo)\\s+(?:called|named|title)?\\s*(.+)", RegexOption.IGNORE_CASE),
            Regex("(?:ek\\s+)?(?:task|kaam)\\s+(?:banao|add karo|create karo)\\s+(?:called|named)?\\s*(.+)", RegexOption.IGNORE_CASE),
            Regex("(.+?)\\s+(?:naam ka|called)?\\s*(?:task|kaam)\\s+(?:banao|add karo)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(clean)
            if (match != null) {
                var title = match.groupValues[1].trim()
                    .replace(Regex("^called\\s+", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^named\\s+", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (title.isNotEmpty()) {
                    return CommandIntent.CreateTask(title)
                }
            }
        }
        return null
    }

    private fun matchProjectStatus(clean: String, lower: String): CommandIntent? {
        val patterns = listOf(
            Regex("(?:project\\s+)?(.+?)\\s+(?:ka\\s+)?status\\s+(?:kya hai|batao|check karo)?", RegexOption.IGNORE_CASE),
            Regex("(?:status of|check status of|what is the status of)\\s+(?:project\\s+)?(.+)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(clean)
            if (match != null) {
                val name = match.groupValues[1].trim()
                    .replace(Regex("^(project)\\s+", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s+ka$", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (name.isNotEmpty() && !name.equals("project", ignoreCase = true)) {
                    return CommandIntent.GetProjectStatus(name)
                }
            }
        }
        return null
    }

    private fun cleanPunctuation(str: String): String {
        return str.replace("?", "").replace("!", "").replace(".", "").trim()
    }

    private fun formatResult(result: ToolExecutionResult): String {
        return when (result) {
            is ToolExecutionResult.Success -> result.message
            is ToolExecutionResult.Error -> "❌ ${result.message}"
            is ToolExecutionResult.ClarificationRequired -> "❓ ${result.message}\n" +
                    result.candidates.joinToString("\n") { "• ${it.title} (${it.details ?: ""})" }
            is ToolExecutionResult.ConfirmationRequired -> "⚠️ ${result.message}"
        }
    }
}
