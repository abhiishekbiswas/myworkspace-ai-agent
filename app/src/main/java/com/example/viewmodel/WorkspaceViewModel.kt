package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.relation.ProjectWithCategoryAndTasks
import com.example.core.database.relation.ProjectWithTasks
import com.example.core.database.relation.TaskWithSubtasksAndDetails
import com.example.data.local.entity.*
import com.example.domain.model.*
import com.example.domain.repository.IWorkspaceRepository
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkspaceViewModel(
    private val repository: IWorkspaceRepository
) : ViewModel() {

    val activeProjects: StateFlow<List<ProjectEntity>> = repository.activeProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedProjects: StateFlow<List<ProjectEntity>> = repository.archivedProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasksWithDeadlines: StateFlow<List<TaskEntity>> = repository.tasksWithDeadlines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentActivities: StateFlow<List<ActivityLogEntity>> = repository.recentActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDependencies: StateFlow<List<TaskDependencyEntity>> = repository.allDependencies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedTaskIds: StateFlow<Set<String>> = combine(allTasks, allDependencies) { tasks, deps ->
        val taskMap = tasks.associateBy { it.id }
        val blocked = mutableSetOf<String>()
        for (dep in deps) {
            val prereq = taskMap[dep.dependsOnTaskId]
            if (prereq != null && prereq.status != "COMPLETED") {
                blocked.add(dep.taskId)
            }
        }
        blocked
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    val projectProgressMap: StateFlow<Map<String, Double>> = activeProjects.map { projects ->
        projects.associate { it.id to it.progress }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val dashboardStats: StateFlow<DashboardStats> = combine(activeProjects, allTasks) { projects, tasks ->
        val completed = tasks.count { it.status == "COMPLETED" }
        val pending = tasks.count { it.status != "COMPLETED" }
        val now = System.currentTimeMillis()
        val overdue = tasks.count { it.dueDate != null && it.dueDate < now && it.status != "COMPLETED" }
        val overall = if (tasks.isNotEmpty()) (completed.toDouble() / tasks.size.toDouble()) * 100.0 else 0.0
        DashboardStats(
            totalProjects = projects.size,
            activeProjects = projects.count { it.status == "ACTIVE" },
            completedProjects = projects.count { it.status == "COMPLETED" },
            totalTasks = tasks.size,
            completedTasks = completed,
            pendingTasks = pending,
            overdueTasks = overdue,
            overallProgress = overall
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    fun selectProject(id: String?) {
        _selectedProjectId.value = id
    }

    private val _selectedTaskId = MutableStateFlow<String?>(null)
    val selectedTaskId: StateFlow<String?> = _selectedTaskId.asStateFlow()

    fun selectTask(id: String?) {
        _selectedTaskId.value = id
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _searchFilter = MutableStateFlow("ALL")
    val searchFilter: StateFlow<String> = _searchFilter.asStateFlow()

    fun setSearchFilter(filter: String) {
        _searchFilter.value = filter
    }

    private val _taskStatusFilter = MutableStateFlow<String?>(null)
    val taskStatusFilter: StateFlow<String?> = _taskStatusFilter.asStateFlow()

    fun setTaskStatusFilter(status: String?) {
        _taskStatusFilter.value = status
    }

    private val _taskSortOption = MutableStateFlow(TaskSortOption.SORT_ORDER)
    val taskSortOption: StateFlow<TaskSortOption> = _taskSortOption.asStateFlow()

    fun setTaskSortOption(sort: TaskSortOption) {
        _taskSortOption.value = sort
    }

    private val _projectCategoryFilter = MutableStateFlow<String?>(null)
    val projectCategoryFilter: StateFlow<String?> = _projectCategoryFilter.asStateFlow()

    fun setProjectCategoryFilter(catId: String?) {
        _projectCategoryFilter.value = catId
    }

    private val _projectStatusFilter = MutableStateFlow<String?>(null)
    val projectStatusFilter: StateFlow<String?> = _projectStatusFilter.asStateFlow()

    fun setProjectStatusFilter(status: String?) {
        _projectStatusFilter.value = status
    }

    private val _projectSortOption = MutableStateFlow(ProjectSortOption.NAME)
    val projectSortOption: StateFlow<ProjectSortOption> = _projectSortOption.asStateFlow()

    fun setProjectSortOption(sort: ProjectSortOption) {
        _projectSortOption.value = sort
    }

    fun getProject(id: String): Flow<ProjectEntity?> = repository.getProjectById(id)
    fun getTasksForProject(id: String): Flow<List<TaskEntity>> = repository.getTasksByProject(id)
    fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>> = repository.getSubtasks(parentTaskId)
    fun getTask(id: String): Flow<TaskEntity?> = repository.getTaskById(id)
    fun getChecklistForTask(taskId: String): Flow<List<ChecklistItemEntity>> = repository.getChecklistForTask(taskId)
    fun getCommentsForTask(taskId: String): Flow<List<CommentEntity>> = repository.getCommentsForTask(taskId)
    fun getProgressLogsForProject(projectId: String): Flow<List<ProgressLogEntity>> = repository.getProgressLogsForProject(projectId)
    fun getProgressLogsForTask(taskId: String): Flow<List<ProgressLogEntity>> = repository.getProgressLogsForTask(taskId)
    fun getNotesForProject(projectId: String): Flow<List<NoteEntity>> = repository.getNotesForProject(projectId)
    fun getDependenciesForTask(taskId: String): Flow<List<TaskDependencyEntity>> = repository.getDependenciesForTask(taskId)
    fun getDependentsOfTask(taskId: String): Flow<List<TaskDependencyEntity>> = repository.getDependentsOfTask(taskId)
    fun getProjectWithTasks(id: String): Flow<ProjectWithTasks?> = repository.getProjectWithTasks(id)
    fun getProjectWithCategoryAndTasks(id: String): Flow<ProjectWithCategoryAndTasks?> = repository.getProjectWithCategoryAndTasks(id)
    fun getTaskWithDetails(id: String): Flow<TaskWithSubtasksAndDetails?> = repository.getTaskWithDetails(id)

    fun saveProject(project: ProjectEntity) = viewModelScope.launch {
        repository.saveProject(project)
    }

    fun duplicateProject(projectId: String, onResult: (Result<ProjectEntity>) -> Unit = {}) = viewModelScope.launch {
        val result = repository.duplicateProject(projectId)
        onResult(result)
    }

    fun archiveProject(projectId: String) = viewModelScope.launch {
        repository.archiveProject(projectId)
    }

    fun restoreProject(projectId: String) = viewModelScope.launch {
        repository.restoreProject(projectId)
    }

    fun deleteProject(project: ProjectEntity) = viewModelScope.launch {
        repository.deleteProject(project)
    }

    fun saveCategory(category: CategoryEntity) = viewModelScope.launch {
        repository.saveCategory(category)
    }

    fun deleteCategory(categoryId: String, reassignToCategoryId: String? = null, onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch {
        val result = repository.deleteCategory(categoryId, reassignToCategoryId)
        onResult(result)
    }

    fun saveTask(task: TaskEntity, onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch {
        val valResult = repository.validateTaskHierarchy(task.id, task.parentTaskId, task.projectId)
        if (valResult.isFailure) {
            onResult(valResult)
            return@launch
        }
        repository.saveTask(task)
        onResult(Result.success(Unit))
    }

    fun updateTaskStatus(taskId: String, newStatus: String) = viewModelScope.launch {
        repository.updateTaskStatus(taskId, newStatus)
    }

    fun updateTaskSortOrder(taskId: String, newSortOrder: Int) = viewModelScope.launch {
        repository.updateTaskSortOrder(taskId, newSortOrder)
    }

    fun deleteTask(task: TaskEntity) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun toggleTaskCompletion(task: TaskEntity) = viewModelScope.launch {
        val newStatus = if (task.status == "COMPLETED") "PENDING" else "COMPLETED"
        repository.updateTaskStatus(task.id, newStatus)
    }

    fun createTask(
        title: String,
        description: String = "",
        projectId: String? = null,
        priority: String = "MEDIUM"
    ) = viewModelScope.launch {
        val pid = projectId ?: activeProjects.value.firstOrNull()?.id ?: "default-project"
        val task = TaskEntity(
            title = title,
            description = description,
            projectId = pid,
            priority = priority,
            status = "PENDING"
        )
        repository.saveTask(task)
    }

    fun createProject(
        name: String,
        description: String = "",
        categoryId: String? = null,
        color: String = "#3B82F6"
    ) = viewModelScope.launch {
        val project = ProjectEntity(
            name = name,
            description = description,
            categoryId = categoryId,
            color = color
        )
        repository.saveProject(project)
    }

    fun createNote(title: String, content: String = "") = viewModelScope.launch {
        val note = NoteEntity(title = title, content = content)
        repository.saveNote(note)
    }

    fun removeDependency(taskId: String, dependsOnTaskId: String) = viewModelScope.launch {
        repository.deleteDependencyPair(taskId, dependsOnTaskId)
    }

    fun addDependency(taskId: String, dependsOnTaskId: String, dependencyType: String = "BLOCKS", onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch {
        val res = repository.addDependency(taskId, dependsOnTaskId, dependencyType)
        onResult(res)
    }

    fun addDependency(taskId: String, dependsOnTaskId: String, onCycleDetected: () -> Unit) = viewModelScope.launch {
        val res = repository.addDependency(taskId, dependsOnTaskId, "BLOCKS")
        if (res.isFailure) {
            onCycleDetected()
        }
    }

    fun deleteDependency(dependencyId: String) = viewModelScope.launch {
        repository.deleteDependency(dependencyId)
    }

    fun deleteDependencyPair(taskId: String, dependsOnTaskId: String) = viewModelScope.launch {
        repository.deleteDependencyPair(taskId, dependsOnTaskId)
    }

    fun saveChecklistItem(item: ChecklistItemEntity) = viewModelScope.launch {
        repository.saveChecklistItem(item)
    }

    fun toggleChecklistItem(item: ChecklistItemEntity) = viewModelScope.launch {
        repository.toggleChecklistItem(item)
    }

    fun deleteChecklistItem(item: ChecklistItemEntity) = viewModelScope.launch {
        repository.deleteChecklistItem(item)
    }

    fun addComment(taskId: String, content: String) = viewModelScope.launch {
        repository.addComment(taskId, content)
    }

    fun deleteComment(comment: CommentEntity) = viewModelScope.launch {
        repository.deleteComment(comment)
    }

    fun saveNote(note: NoteEntity) = viewModelScope.launch {
        repository.saveNote(note)
    }

    fun deleteNote(note: NoteEntity) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    fun recordProgressUpdate(projectId: String, newProgress: Double, updateNote: String = "", source: String = "MANUAL") = viewModelScope.launch {
        repository.recordProgressUpdate(projectId, newProgress, updateNote, source)
    }

    fun exportBackup(onResult: (String) -> Unit) = viewModelScope.launch {
        val json = repository.exportBackupJson()
        onResult(json)
    }

    fun restoreBackup(json: String, onResult: (Result<WorkspaceBackup>) -> Unit) = viewModelScope.launch {
        val result = repository.restoreBackupJson(json)
        onResult(result)
    }

    init {
        viewModelScope.launch {
            repository.seedInitialData()
        }
    }
}

class WorkspaceViewModelFactory(
    private val repository: IWorkspaceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkspaceViewModel::class.java)) {
            return WorkspaceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
