# Changelog — My Workspace

All notable changes to the "My Workspace" application are documented in this file.

## [Phase 3: Advanced Task Management & Dependency Engine] - November 1, 2026

### Added
- **Directed Task Dependencies (`TaskDependencyEntity`):**
  - Compound primary key `(taskId, dependsOnTaskId)` with indexed foreign keys referencing `tasks(id)` with `CASCADE` delete.
  - Cycle detection and validation in `WorkspaceRepository.addDependency()` using BFS traversal to prevent circular dependency chains.
  - Rejection of self-dependencies (`taskId == dependsOnTaskId`).
  - Automatic dependency remapping when duplicating projects.
- **Blocked State Engine & Guard Rails:**
  - `isTaskBlocked` evaluation: marks any task as blocked if any of its prerequisite tasks are incomplete (`status != "Completed"`).
  - Amber "Blocked" chip badge on `TaskCard` in `TasksScreen` and `DashboardScreen`.
  - Guard rail `AlertDialog` in `TasksScreen` warning the user when attempting to mark a blocked task as completed, with options to proceed anyway or cancel.
- **Dependency Management UI in `TaskDetailScreen`:**
  - Dedicated "Task Dependencies" card section.
  - Prominent blocked status warning banner displaying incomplete prerequisite count.
  - Prerequisites list with status indicators, title, priority, and one-tap removal.
  - Downstream dependents list displaying tasks waiting on this task.
  - Modal prerequisite picker with search and exclusion of self and already-linked tasks.
- **Automated Testing Suite:**
  - Added `testTaskDependenciesAndBlockedStatus` in `DatabaseRepositoryTest` covering self-dependency rejection, cycle prevention, prerequisite completion status transitions, dependent retrieval, and cascade deletion.

## [Phase 2: Database & Core Storage Integration] - October 31, 2026

### Added
- **Production Room Database (`WorkspaceDatabase`):**
  - Configured Room SQLite database with schema version 2, full offline persistence, and reactive `Flow` data streams.
  - Added SQLite Foreign Key constraints with `CASCADE` and `SET_NULL` integrity rules.
  - Indexed critical query columns (`categoryId`, `projectId`, `parentTaskId`, `status`, `dueDate`, `priority`, `sortOrder`, `isArchived`).
- **Entity Suite & Domain Mapping:**
  - `CategoryEntity`, `ProjectEntity`, `TaskEntity`, `ChecklistItemEntity`, `CommentEntity`, `NoteEntity`, `ProgressLogEntity`, `ActivityLogEntity`.
  - Type-safe domain models and bidirectional mappers in `WorkspaceMappers.kt`.
  - Room Relations: `ProjectWithTasks`, `ProjectWithCategoryAndTasks`, `TaskWithSubtasksAndDetails`.
- **Specialized DAOs & Clean Repository Architecture:**
  - `ProjectDao`, `CategoryDao`, `TaskDao`, `ChecklistItemDao`, `CommentDao`, `NoteDao`, `ProgressLogDao`, `ActivityLogDao`.
  - Unified `IWorkspaceRepository` domain contract and `WorkspaceRepository` implementation.
  - Domain use cases for projects, tasks, categories, checklists, and comments.
- **Enterprise Features & Data Integrity:**
  - **Atomic Project Duplication:** Clone any project and all associated tasks; remaps hierarchical subtask references to new cloned task UUIDs and duplicates checklist items.
  - **Category Deletion Protection:** Guards against deleting categories in use by existing projects unless explicitly reassigned.
  - **Task Hierarchy Validation & Cycle Detection:** Algorithmic cycle prevention preventing infinite parent loops, self-parenting, and cross-project subtask assignments.
  - **Reactive Automatic Progress Calculation:** Dynamically updates task completion percentages and status transitions (`In Progress`, `Completed`) when checklist items change.
- **Automated Testing Suite:**
  - Added `DatabaseRepositoryTest` with Robolectric local JVM testing covering in-memory Room database operations, CRUD flows, category protection, cycle detection, project duplication, and checklist calculation.

### Verified
- Zero compilation errors (`compile_applet` PASS).
- Unit & Robolectric test suite passing 100% via `gradle :app:testDebugUnitTest`.

---

## [Phase 1: Foundation, Premium UI & Navigation] - October 31, 2026

### Added
- **5-Destination Bottom Navigation:**
  - Integrated `Home` (Dashboard), `Projects`, `Tasks`, `Calendar`, and `More` bottom bar destinations with Material 3 styling and active indicators.
  - Added `MoreScreen` providing full access to Productivity Analytics, Notes & Scratchpad, Global Search, Backup & Restore, Theme Settings, and Workspace metrics.
- **Header & Navigation Bar (`AppTopBar`):**
  - Time-of-day greeting ("Welcome Back"), formatted current date, instant search trigger, notification bell with unread badge counter, and settings navigation.
  - Interactive notification alert dialog summarizing pending, high-priority, and overdue items.
- **Quick Add Menu (`QuickAddMenu`):**
  - Floating Action Button trigger opening a Material 3 ModalBottomSheet with 4 command actions:
    1. New Project (opens project creation modal)
    2. New Task (opens task creation modal with project assignment)
    3. New Note (opens quick note capture modal)
    4. Progress Update (opens milestone logger with slider and handoff notes)
- **Multi-Theme System:**
  - Complete support for 3 themes: Dark Theme (Slate/Indigo default), AMOLED Black Theme (true #000000 high-contrast), and Light Theme.
  - StateFlow-driven theme mode persistence and switching without app restart.
- **Premium M3 Dashboard:**
  - Overall weighted progress card with animated circular indicator.
  - 2x2 statistics grid with clickable cards for Active Projects, Completed, Pending Tasks, and Overdue Tasks.
  - Today's Focus & Deadlines list with priority pills, status badges, and direct detail routing.
  - Active Projects horizontal overview with category pills and completion bars.
  - Chronological audit activity stream.
- **Modular Component Suite (`ui/components`):**
  - `StatisticsCard`, `ProjectProgressCard`, `TaskItem`, `PriorityBadge`, `StatusBadge`, `SectionHeader`, `EmptyState`, `AppTopBar`, `QuickAddMenu`.
- **Custom Adaptive Launcher Icon:**
  - Generated high-resolution adaptive vector foreground, background, and mipmap rasters across MDPI, HDPI, XHDPI, XXHDPI, and XXXHDPI.

### Verified
- Zero compilation errors (`compile_applet` PASS).
- Standard unit and Robolectric tests passing 100%.
