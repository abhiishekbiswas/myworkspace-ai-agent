# Master Developer Handbook: My Workspace

## Project Overview
**My Workspace** is an offline-first productivity platform built with modern Android native technologies (Kotlin, Jetpack Compose, Material 3, Room, MVVM).

---

## 1. Architecture & Technology Stack
- **OS Platform:** Android (minSdk 24, targetSdk 36)
- **UI Toolkit:** Jetpack Compose (BOM 2024.09.00+)
- **Design System:** Material Design 3 (M3)
- **Architecture:** Clean Architecture / MVVM
  - `data/local`: Room entities, DAOs, and Database
  - `data/repository`: Single source of truth repository
  - `viewmodel`: WorkspaceViewModel managing reactive StateFlows
  - `ui/navigation`: Typed Compose navigation routes and MainNavigation host
  - `ui/screens`: Focused, modular screen composables
  - `ui/components`: Reusable UI elements, badges, cards, and modal dialogs
  - `ui/theme`: Multi-theme engine (Dark Slate, AMOLED Black, Light)
  - `core/designsystem`: Shapes, typography, elevation, and design tokens
  - `core/common`: Isolated sample data and helpers

---

## 2. Phase Roadmap
- **Phase 1: Foundation, Premium UI & Navigation (COMPLETED)**
  - Application theme system (Dark default, AMOLED Black, Light).
  - Main Navigation structure with 5 bottom navigation destinations (Home, Projects, Tasks, Calendar, More).
  - Premium Dashboard UI with Header, Statistics, Overall Progress, Today's Tasks, Active Projects, Recent Activity, and Quick Add Menu.
  - Initial screens for Projects, Tasks, Calendar, and More with empty states.
  - Component library: `AppTopBar`, `BottomNavigation`, `ProjectProgressCard`, `StatisticsCard`, `TaskItem`, `PriorityBadge`, `StatusBadge`, `SectionHeader`, `EmptyState`, `QuickAddMenu`.
  - Custom adaptive launcher icons.
- **Phase 2: Database & Core Storage Integration**
- **Phase 3: Hierarchical Tasks, Checklists & Work Session Logging**
- **Phase 4: Advanced Views (Kanban Board, Calendar Deadlines, Analytics)**
- **Phase 5: Search, Backup/Restore & Export**
- **Phase 6: Polish, Accessibility & Optimization**

---

## 3. UI Identification & Testing Standards
- All primary interactive elements are tagged with `Modifier.testTag("...")` following snake_case convention.
- Minimum touch target: 48dp x 48dp.
- Consistent 8dp spacing grid used across all screens.
